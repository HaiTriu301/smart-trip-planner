package com.trieu.tripplanner.provider.map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trieu.tripplanner.config.properties.ProviderProperties;
import com.trieu.tripplanner.exception.ProviderUnavailableException;
import com.trieu.tripplanner.model.enums.ActivityType;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import com.trieu.tripplanner.provider.map.dto.RouteLeg;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Real map source: the public OpenStreetMap services (design.md 7.2). No API key. Active when
 * {@code app.providers.map=osm}.
 * <ul>
 *   <li>{@link #search}: Photon, a search service made for suggestions while typing;</li>
 *   <li>{@link #lookup}: Nominatim, asked for one place by its OpenStreetMap id at the moment a user picks a
 *       result. Its terms forbid using it for suggestions, which is why two services are needed;</li>
 *   <li>{@link #route}: OSRM, the way by road through all the stops of a day in one call.</li>
 * </ul>
 * The id of a place is the letter of its OpenStreetMap object type followed by its number, for example
 * {@code W204885903}: what Photon tells in two fields and exactly what Nominatim accepts.
 * <p>
 * Names come in the language of the place itself (Vietnamese in Việt Nam): no language is asked for, Photon
 * knows only English, German and French besides that default.
 * <p>
 * Each service has its own retry and its own circuit (design.md 7.3; the numbers are in application.yml under
 * {@code resilience4j}): one of them being down does not stop the other two. The annotations work through the
 * proxy Spring puts in front of this bean, so they only apply to calls coming from another bean. A call that is
 * not let through, because the circuit is open or the rate limit is reached, ends as the same
 * ProviderUnavailableException as any other failure: callers never see an exception of the library.
 */
@Component
@ConditionalOnProperty(name = "app.providers.map", havingValue = "osm")
public class OsmMapProvider implements MapProvider {

    private static final String PHOTON = "photon";
    private static final String NOMINATIM = "nominatim";
    private static final String OSRM = "osrm";

    /**
     * The route service of OSRM for the car. The public server has no other means of travel: it gives the same
     * answer whatever profile the address names (checked 2026-10-05).
     */
    private static final String ROUTE_PATH = "/route/v1/driving/";
    private static final String OSRM_OK = "Ok";

    /** N node, W way, R relation, then the number OpenStreetMap gave the object. */
    private static final Pattern EXTERNAL_ID = Pattern.compile("[NWR][0-9]{1,19}");

    /** The columns of the places table: DECIMAL(10,7), VARCHAR(200), VARCHAR(500). */
    private static final int COORDINATE_SCALE = 7;
    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_ADDRESS_LENGTH = 500;

    private static final ParameterizedTypeReference<List<NominatimPlace>> NOMINATIM_PLACES =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient photon;
    private final RestClient nominatim;
    private final RestClient osrm;

    /**
     * @param builder the application's builder: it carries the time limits of {@code spring.http.clients.*}
     */
    public OsmMapProvider(RestClient.Builder builder, ProviderProperties properties) {
        RestClient.Builder identified = builder.defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent());
        this.photon = identified.clone().baseUrl(properties.osm().photonBaseUrl()).build();
        this.nominatim = identified.clone().baseUrl(properties.osm().nominatimBaseUrl()).build();
        this.osrm = identified.clone().baseUrl(properties.osm().osrmBaseUrl()).build();
    }

    @Override
    public PlaceProvider provider() {
        return PlaceProvider.OSM;
    }

    /**
     * One call to Photon. Its order is kept: it already puts better matches first and, with a reference point,
     * matches around that point before the others. A result without a name, an id or a position cannot be
     * shown or picked and is left out, so fewer than {@code limit} places may come back.
     *
     * @throws ProviderUnavailableException Photon could not be reached, took too long, answered with an error
     *                                      status or sent something unreadable
     */
    @Override
    @Retry(name = PHOTON)
    @CircuitBreaker(name = PHOTON, fallbackMethod = "photonSuspended")
    public List<PlaceResult> search(String query, int limit, Coordinate near) {
        // Values go in as variables, never as part of the address: "&" or "=" typed by a user stay in the keyword
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("q", query);
        values.put("limit", limit);
        if (near != null) {
            values.put("lat", near.lat().toPlainString());
            values.put("lon", near.lng().toPlainString());
        }
        PhotonAnswer answer;
        try {
            answer = photon.get()
                    .uri(uri -> {
                        uri.path("/api/");
                        values.keySet().forEach(name -> uri.queryParam(name, "{" + name + "}"));
                        return uri.build(values);
                    })
                    .retrieve()
                    .body(PhotonAnswer.class);
        }
        catch (RestClientException ex) {
            throw new ProviderUnavailableException(PHOTON, ex.getMessage(), ex);
        }
        if (answer == null || answer.features() == null) {
            throw new ProviderUnavailableException(PHOTON, "the answer has no list of places");
        }
        return answer.features().stream()
                .map(OsmMapProvider::toResult)
                .flatMap(Optional::stream)
                .limit(limit)
                .toList();
    }

    /**
     * One call to Nominatim. An id that does not have the form this source gives out is unknown without asking:
     * the id comes from a client, and Nominatim would read a comma in it as a list of several places.
     * <p>
     * At most one call a second for the whole application, as the terms of the public server demand. A call
     * waits up to two seconds for its turn; when the queue is longer than that, it fails at once.
     *
     * @throws ProviderUnavailableException Nominatim could not be reached, took too long, answered with an
     *                                      error status or sent something unreadable
     */
    @Override
    @Retry(name = NOMINATIM)
    @CircuitBreaker(name = NOMINATIM, fallbackMethod = "nominatimSuspended")
    @RateLimiter(name = NOMINATIM, fallbackMethod = "nominatimQueueFull")
    public Optional<PlaceResult> lookup(String externalId) {
        if (externalId == null || !EXTERNAL_ID.matcher(externalId).matches()) {
            return Optional.empty();
        }
        List<NominatimPlace> places;
        try {
            places = nominatim.get()
                    .uri(uri -> uri.path("/lookup")
                            .queryParam("osm_ids", "{id}")
                            .queryParam("format", "jsonv2")
                            .queryParam("addressdetails", 1)
                            .build(externalId))
                    .retrieve()
                    .body(NOMINATIM_PLACES);
        }
        catch (RestClientException ex) {
            throw new ProviderUnavailableException(NOMINATIM, ex.getMessage(), ex);
        }
        if (places == null) {
            throw new ProviderUnavailableException(NOMINATIM, "the answer is empty");
        }
        // An unknown id is answered with an empty list, not with an error
        return places.stream().findFirst().flatMap(place -> toResult(externalId, place));
    }

    /**
     * One call to OSRM for the whole list of points. Its answer has one leg between every two consecutive points,
     * in the order sent; two points at the same position give a leg of zero, not an error. A point away from
     * any road is moved to the nearest one before the way is computed.
     *
     * @throws ProviderUnavailableException OSRM could not be reached, took too long, answered with an error
     *                                      status, sent something unreadable, or sent another number of legs
     *                                      than one fewer than the points: the caller matches legs to
     *                                      activities by position, a wrong count would put numbers between the
     *                                      wrong activities
     */
    @Override
    @Retry(name = OSRM)
    @CircuitBreaker(name = OSRM, fallbackMethod = "osrmSuspended")
    public List<RouteLeg> route(List<Coordinate> points) {
        if (points.size() < 2) {
            return List.of();
        }
        // Longitude first, ";" between points. Only digits, "." and "-" get into the address: these are numbers
        // read from the database, not text typed by a user
        String path = points.stream()
                .map(point -> point.lng().toPlainString() + "," + point.lat().toPlainString())
                .collect(Collectors.joining(";", ROUTE_PATH, ""));
        OsrmAnswer answer;
        try {
            answer = osrm.get()
                    // Without the drawn line of the way: only the numbers are used, and the answer stays small
                    .uri(uri -> uri.path(path).queryParam("overview", "false").build())
                    .retrieve()
                    .body(OsrmAnswer.class);
        }
        catch (RestClientException ex) {
            throw new ProviderUnavailableException(OSRM, ex.getMessage(), ex);
        }
        if (answer == null || !OSRM_OK.equals(answer.code()) || answer.routes() == null || answer.routes().isEmpty()
                || answer.routes().get(0) == null || answer.routes().get(0).legs() == null) {
            throw new ProviderUnavailableException(OSRM, "the answer has no route");
        }
        List<OsrmLeg> legs = answer.routes().get(0).legs();
        if (legs.size() != points.size() - 1) {
            throw new ProviderUnavailableException(OSRM,
                    "expected " + (points.size() - 1) + " legs, the answer has " + legs.size());
        }
        return legs.stream().map(OsmMapProvider::toLeg).toList();
    }

    // What Resilience4j calls instead of the method when it does not let the call through. One per method: a
    // fallback must have the return type of the method it stands in for. Public on purpose: the library opens
    // a private fallback for the time of one call and closes it again, and two calls at the same moment trip
    // over each other (IllegalAccessException instead of the fallback, BUG-PLAT-004)

    public List<PlaceResult> photonSuspended(CallNotPermittedException ex) {
        throw suspended(PHOTON);
    }

    public Optional<PlaceResult> nominatimSuspended(CallNotPermittedException ex) {
        throw suspended(NOMINATIM);
    }

    public Optional<PlaceResult> nominatimQueueFull(RequestNotPermitted ex) {
        throw new ProviderUnavailableException(NOMINATIM, "too many lookups are waiting for their turn");
    }

    public List<RouteLeg> osrmSuspended(CallNotPermittedException ex) {
        throw suspended(OSRM);
    }

    /** No cause on purpose: a call that was never made is neither retried nor counted against the service. */
    private static ProviderUnavailableException suspended(String source) {
        return new ProviderUnavailableException(source, "calls are suspended after repeated failures");
    }

    /** OSRM gives metres and seconds with one decimal; the application keeps whole metres and seconds. */
    private static RouteLeg toLeg(OsrmLeg leg) {
        if (leg == null || !isAmount(leg.distance()) || !isAmount(leg.duration())) {
            throw new ProviderUnavailableException(OSRM, "a leg has no usable distance or duration");
        }
        return new RouteLeg(Math.round(leg.distance()), Math.round(leg.duration()));
    }

    private static boolean isAmount(Double value) {
        return value != null && Double.isFinite(value) && value >= 0;
    }

    private static Optional<PlaceResult> toResult(PhotonFeature feature) {
        PhotonProperties properties = feature.properties();
        List<BigDecimal> position = (feature.geometry() == null) ? null : feature.geometry().coordinates();
        if (properties == null || isBlank(properties.name()) || properties.osmId() == null
                || isBlank(properties.osmType()) || position == null || position.size() < 2
                || position.get(0) == null || position.get(1) == null) {
            return Optional.empty();
        }
        String externalId = properties.osmType() + properties.osmId();
        if (!EXTERNAL_ID.matcher(externalId).matches()) {
            return Optional.empty();
        }
        String address = joined(
                street(properties.houseNumber(), properties.street()),
                properties.district(),
                firstNonBlank(properties.city(), properties.county()),
                properties.state());
        // GeoJSON order: longitude first
        return Optional.of(new PlaceResult(PlaceProvider.OSM, externalId, cut(properties.name().trim(), MAX_NAME_LENGTH),
                address, degrees(position.get(1)), degrees(position.get(0)),
                categoryOf(properties.osmKey(), properties.osmValue())));
    }

    private static Optional<PlaceResult> toResult(String externalId, NominatimPlace place) {
        if (place.lat() == null || place.lon() == null) {
            return Optional.empty();
        }
        Map<String, String> parts = (place.address() == null) ? Map.of() : place.address();
        String name = firstNonBlank(place.name(), firstPartOf(place.displayName()));
        if (name == null) {
            return Optional.empty();
        }
        String address = joined(
                street(parts.get("house_number"), parts.get("road")),
                firstNonBlank(parts.get("suburb"), parts.get("city_district")),
                firstNonBlank(parts.get("city"), parts.get("town"), parts.get("village"), parts.get("county")),
                parts.get("state"));
        return Optional.of(new PlaceResult(PlaceProvider.OSM, externalId, cut(name.trim(), MAX_NAME_LENGTH), address,
                degrees(place.lat()), degrees(place.lon()), categoryOf(place.category(), place.type())));
    }

    /**
     * The activity type an OpenStreetMap tag suggests, as the name of an {@link ActivityType}, or null when the
     * tag says nothing useful. The UI uses it for the icon of a suggestion and to propose the type of a new
     * activity; a wrong guess costs the user one click, so only clear cases are mapped. Photon calls the two
     * halves of the tag osm_key / osm_value, Nominatim calls them category / type.
     */
    static String categoryOf(String key, String value) {
        if (key == null || value == null) {
            return null;
        }
        ActivityType type = switch (key) {
            case "amenity" -> switch (value) {
                case "restaurant", "cafe", "fast_food", "food_court", "bar", "pub", "ice_cream", "biergarten" ->
                        ActivityType.FOOD;
                case "marketplace" -> ActivityType.SHOPPING;
                case "bus_station", "ferry_terminal", "taxi", "car_rental", "parking", "fuel" ->
                        ActivityType.TRANSPORT;
                case "place_of_worship", "theatre", "cinema", "arts_centre" -> ActivityType.SIGHTSEEING;
                default -> null;
            };
            case "tourism" -> switch (value) {
                case "hotel", "hostel", "guest_house", "motel", "apartment", "resort", "chalet", "camp_site" ->
                        ActivityType.ACCOMMODATION;
                case "information" -> null;
                default -> ActivityType.SIGHTSEEING;        // attraction, museum, viewpoint, zoo, theme_park...
            };
            case "shop" -> ActivityType.SHOPPING;
            case "historic" -> ActivityType.SIGHTSEEING;
            case "natural" -> switch (value) {
                case "beach", "peak", "cave_entrance", "waterfall", "volcano" -> ActivityType.SIGHTSEEING;
                default -> null;
            };
            case "leisure" -> switch (value) {
                case "park", "garden", "nature_reserve", "water_park" -> ActivityType.SIGHTSEEING;
                default -> null;
            };
            case "aeroway" -> switch (value) {
                case "aerodrome", "terminal" -> ActivityType.TRANSPORT;
                default -> null;
            };
            case "railway" -> switch (value) {
                case "station", "halt" -> ActivityType.TRANSPORT;
                default -> null;
            };
            case "highway" -> "bus_stop".equals(value) ? ActivityType.TRANSPORT : null;
            default -> null;
        };
        return (type == null) ? null : type.name();
    }

    /** "119" and "Trần Phú" give "119 Trần Phú"; a house number without a street says nothing. */
    private static String street(String houseNumber, String street) {
        if (isBlank(street)) {
            return null;
        }
        return isBlank(houseNumber) ? street.trim() : houseNumber.trim() + " " + street.trim();
    }

    /** The parts that exist, separated by commas; null when there is none. */
    private static String joined(String... parts) {
        String text = Stream.of(parts)
                .filter(part -> !isBlank(part))
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(", "));
        return text.isEmpty() ? null : cut(text, MAX_ADDRESS_LENGTH);
    }

    private static String firstNonBlank(String... values) {
        return Stream.of(values).filter(value -> !isBlank(value)).findFirst().orElse(null);
    }

    /** "Chợ Hàn, 119, Trần Phú, ..." gives "Chợ Hàn": the name of a place Nominatim has no name field for. */
    private static String firstPartOf(String displayName) {
        return isBlank(displayName) ? null : displayName.split(",", 2)[0];
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private static String cut(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private static BigDecimal degrees(BigDecimal value) {
        return value.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
    }

    /** The part of a Photon answer this application reads: a GeoJSON collection of places. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PhotonAnswer(List<PhotonFeature> features) {

        PhotonAnswer {
            // A null element would be a broken answer of its own; nothing to keep of it
            features = (features == null) ? null : features.stream().filter(Objects::nonNull).toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PhotonFeature(PhotonProperties properties, PhotonGeometry geometry) {
    }

    /** @param coordinates longitude, then latitude */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record PhotonGeometry(List<BigDecimal> coordinates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PhotonProperties(
            @JsonProperty("osm_type") String osmType,
            @JsonProperty("osm_id") Long osmId,
            @JsonProperty("osm_key") String osmKey,
            @JsonProperty("osm_value") String osmValue,
            String name,
            @JsonProperty("housenumber") String houseNumber,
            String street,
            String district,
            String city,
            String county,
            String state) {
    }

    /**
     * The part of an OSRM answer this application reads.
     *
     * @param code   "Ok" when a way was found; anything else names what went wrong
     * @param routes the ways found; the first one is the best, and the only one unless alternatives are asked for
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmAnswer(String code, List<OsrmRoute> routes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmRoute(List<OsrmLeg> legs) {
    }

    /**
     * @param distance metres by road
     * @param duration seconds by car
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmLeg(Double distance, Double duration) {
    }

    /** One element of a Nominatim lookup answer; lat and lon come as text. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record NominatimPlace(
            BigDecimal lat,
            BigDecimal lon,
            String name,
            @JsonProperty("display_name") String displayName,
            String category,
            String type,
            Map<String, String> address) {
    }

}
