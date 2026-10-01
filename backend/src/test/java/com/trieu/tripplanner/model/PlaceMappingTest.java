package com.trieu.tripplanner.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Checks Place.java against V9__create_places.sql on a real MySQL (Testcontainers, Flyway, ddl-auto=validate),
 * plus the constraints the service relies on. Each test runs in a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PlaceMappingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void everyColumnSurvivesARoundTrip() {
        Place saved = entityManager.persistAndFlush(linhUng().build());
        entityManager.clear();

        Place found = entityManager.find(Place.class, saved.getId());

        assertThat(found.getProvider()).isEqualTo(PlaceProvider.MOCK);
        assertThat(found.getExternalId()).isEqualTo("da-nang-chua-linh-ung");
        assertThat(found.getName()).isEqualTo("Chùa Linh Ứng");
        assertThat(found.getAddress()).isEqualTo("Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng");
        // DECIMAL(10,7): seven decimals are kept exactly, about one centimetre on the ground
        assertThat(found.getLat()).isEqualByComparingTo("16.1001567");
        assertThat(found.getLng()).isEqualByComparingTo("108.2784112");
        assertThat(found.getCategory()).isEqualTo("SIGHTSEEING");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void addressAndCategoryAreOptional() {
        Place saved = entityManager.persistAndFlush(linhUng().address(null).category(null).build());
        entityManager.clear();

        Place found = entityManager.find(Place.class, saved.getId());
        assertThat(found.getAddress()).isNull();
        assertThat(found.getCategory()).isNull();
    }

    @Test
    void samePlaceOfTheSameSourceCannotBeStoredTwice() {
        entityManager.persistAndFlush(linhUng().build());

        // The second copy is what two simultaneous "pick this result" requests would try to write
        assertThatThrownBy(() -> entityManager.persistAndFlush(linhUng().name("Bản chép thứ hai").build()))
                .isInstanceOf(PersistenceException.class)
                .hasMessageContaining("uk_places_provider_external_id");
    }

    @Test
    void externalIdIsComparedExactlyIncludingCase() {
        // An identifier is not text: "W123" and "w123" may be two different places of a source
        entityManager.persistAndFlush(linhUng().externalId("W123").build());
        entityManager.persistAndFlush(linhUng().externalId("w123").build());

        assertThat(countPlaces()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({
            "90.0000001, 108, chk_places_lat",
            "-90.0000001, 108, chk_places_lat",
            "16, 180.0000001, chk_places_lng",
            "16, -180.0000001, chk_places_lng"})
    void coordinatesOutsideTheGlobeAreRejectedByTheDatabase(String lat, String lng, String brokenConstraint) {
        // Last line of defence under the request validation. Spring does not translate MySQL's CHECK error (3819)
        // into DataIntegrityViolationException, so the test names the constraint instead of the exception type
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO places (provider, external_id, name, lat, lng, created_at, updated_at)
                VALUES ('MOCK', 'out-of-range', 'Ngoài bản đồ', ?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, new BigDecimal(lat), new BigDecimal(lng)))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining(brokenConstraint);
    }

    @Test
    void edgesOfTheGlobeAreAccepted() {
        entityManager.persistAndFlush(linhUng().lat(new BigDecimal("90")).lng(new BigDecimal("-180")).build());

        assertThat(countPlaces()).isEqualTo(1);
    }

    @Test
    void manualPlaceKeepsItsCreatorAndNeedsNoExternalId() {
        User creator = entityManager.persist(User.builder()
                .email("creator@example.com").passwordHash("$2a$12$hash").fullName("Người tạo").build());
        Place saved = entityManager.persistAndFlush(Place.builder()
                .provider(PlaceProvider.MANUAL)
                .name("Nhà bà ngoại")
                .lat(new BigDecimal("16.0471234"))
                .lng(new BigDecimal("108.2068765"))
                .createdBy(creator)
                .build());
        entityManager.clear();

        Place found = entityManager.find(Place.class, saved.getId());

        assertThat(found.getProvider()).isEqualTo(PlaceProvider.MANUAL);
        assertThat(found.getExternalId()).isNull();
        assertThat(found.getCreatedBy().getId()).isEqualTo(creator.getId());
    }

    @Test
    void placeCopiedFromASourceHasNoCreator() {
        Place saved = entityManager.persistAndFlush(linhUng().build());
        entityManager.clear();

        // Shared by everyone who picks it, owned by nobody
        assertThat(entityManager.find(Place.class, saved.getId()).getCreatedBy()).isNull();
    }

    @Test
    void enumColumnAlreadyAcceptsTheSourcesOfLaterTasks() {
        // V9 declares OSM although the Java enum does not have it yet: adding the constant in Task 3.8 needs no
        // new migration
        jdbcTemplate.update("""
                INSERT INTO places (provider, external_id, name, lat, lng, created_at, updated_at)
                VALUES ('OSM', 'W1', 'Từ OpenStreetMap', 16, 108, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
                       ('MANUAL', NULL, 'Tự thêm 1', 16, 108, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
                       ('MANUAL', NULL, 'Tự thêm 2', 16, 108, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """);

        // Two MANUAL rows without external_id do not collide on the UNIQUE key
        assertThat(countPlaces()).isEqualTo(3);
    }

    private Integer countPlaces() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM places", Integer.class);
    }

    private static Place.PlaceBuilder linhUng() {
        return Place.builder()
                .provider(PlaceProvider.MOCK)
                .externalId("da-nang-chua-linh-ung")
                .name("Chùa Linh Ứng")
                .address("Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng")
                .lat(new BigDecimal("16.1001567"))
                .lng(new BigDecimal("108.2784112"))
                .category("SIGHTSEEING");
    }

}
