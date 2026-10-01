package com.trieu.tripplanner.model;

import com.trieu.tripplanner.model.enums.PlaceProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The app's own copy of a place (design.md 5.2 "places", rule 14.18). Column types must match
 * V9__create_places.sql exactly (ddl-auto=validate).
 * <p>
 * A row is written once, when a user picks a search result, and never edited afterwards: there are no setters.
 * The pair {@code provider} + {@code externalId} is unique, so everybody who picks the same place of a source
 * shares one row.
 */
@Entity
@Table(name = "places")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Place extends BaseEntity {

    // Hibernate 7 maps @Enumerated(STRING) to the native MySQL ENUM type, matching the migration
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private PlaceProvider provider;

    /** The id of the place inside its source; with {@code provider} it names the place for good. */
    @Column(name = "external_id", length = 128)
    private String externalId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    /** One of the ActivityType names for the mock data; a real source may send anything, or nothing. */
    @Column(name = "category", length = 40)
    private String category;

}
