import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Parking lot - the most common object-oriented (low-level) design question.
 *
 *   java system-design/04_low_level_design/ParkingLot.java
 *
 * HOW TO ANSWER ANY LLD QUESTION:
 *  1. Clarify: vehicle types? spot sizes? levels? pricing? payment? concurrency?
 *  2. Nouns -> classes/enums (Vehicle, ParkingSpot, Level, Ticket, ParkingLot). Verbs -> methods (park, unpark).
 *  3. Put behaviour that varies behind an interface (PricingStrategy) -> Strategy pattern, Open/Closed principle.
 *  4. Handle concurrency at the critical section (two cars grabbing the same spot).
 *
 * TRICKS here:
 *  - Vehicle fits the SMALLEST free spot that is big enough (enum ordinal comparison).
 *  - Free spots kept per size in queues -> O(1) find instead of scanning every spot.
 *  - park/unpark are synchronized; in a distributed system this becomes a conditional DB update.
 */
public class ParkingLot {

    enum SpotSize { SMALL, MEDIUM, LARGE }

    enum VehicleType {
        MOTORCYCLE(SpotSize.SMALL), CAR(SpotSize.MEDIUM), TRUCK(SpotSize.LARGE);

        final SpotSize minSpot;
        VehicleType(SpotSize minSpot) { this.minSpot = minSpot; }
    }

    record Vehicle(String plate, VehicleType type) {}

    record Ticket(long id, Vehicle vehicle, String spotId, LocalDateTime entryTime) {}

    record Receipt(Ticket ticket, LocalDateTime exitTime, long feeCents) {
        @Override
        public String toString() {
            return String.format("%s %s spot=%s %d min -> $%d.%02d", ticket.vehicle().type(), ticket.vehicle().plate(),
                    ticket.spotId(), Duration.between(ticket.entryTime(), exitTime).toMinutes(), feeCents / 100, feeCents % 100);
        }
    }

    static class ParkingSpot {
        final String id;
        final SpotSize size;
        Vehicle vehicle;

        ParkingSpot(String id, SpotSize size) {
            this.id = id;
            this.size = size;
        }
    }

    // STRATEGY: pricing can change (hourly, flat, weekend) without touching ParkingLot
    interface PricingStrategy {
        long feeCents(VehicleType type, Duration parked);
    }

    static class HourlyPricing implements PricingStrategy {
        private final Map<VehicleType, Long> centsPerHour = Map.of(
                VehicleType.MOTORCYCLE, 100L, VehicleType.CAR, 300L, VehicleType.TRUCK, 600L);

        public long feeCents(VehicleType type, Duration parked) {
            long hours = Math.max(1, (parked.toMinutes() + 59) / 60); // round UP, minimum 1 hour
            return hours * centsPerHour.get(type);
        }
    }

    private final Map<SpotSize, Deque<ParkingSpot>> freeSpots = new EnumMap<>(SpotSize.class);
    private final Map<String, ParkingSpot> spotsById = new HashMap<>();
    private final Map<Long, Ticket> activeTickets = new HashMap<>();
    private final Set<String> parkedPlates = new HashSet<>();
    private final PricingStrategy pricing;
    private final AtomicLong ticketIds = new AtomicLong(1);

    ParkingLot(int levels, int smallPerLevel, int mediumPerLevel, int largePerLevel, PricingStrategy pricing) {
        this.pricing = pricing;
        for (SpotSize s : SpotSize.values()) freeSpots.put(s, new ArrayDeque<>());
        for (int level = 1; level <= levels; level++) {
            addSpots(level, SpotSize.SMALL, smallPerLevel);
            addSpots(level, SpotSize.MEDIUM, mediumPerLevel);
            addSpots(level, SpotSize.LARGE, largePerLevel);
        }
    }

    private void addSpots(int level, SpotSize size, int count) {
        for (int i = 1; i <= count; i++) {
            ParkingSpot spot = new ParkingSpot("L" + level + "-" + size.name().charAt(0) + i, size);
            spotsById.put(spot.id, spot);
            freeSpots.get(size).add(spot);
        }
    }

    synchronized Optional<Ticket> park(Vehicle vehicle, LocalDateTime now) {
        if (parkedPlates.contains(vehicle.plate())) throw new IllegalStateException(vehicle.plate() + " is already parked");
        for (SpotSize size : SpotSize.values()) {
            if (size.ordinal() < vehicle.type().minSpot.ordinal()) continue; // too small
            ParkingSpot spot = freeSpots.get(size).poll();
            if (spot != null) {
                spot.vehicle = vehicle;
                Ticket ticket = new Ticket(ticketIds.getAndIncrement(), vehicle, spot.id, now);
                activeTickets.put(ticket.id(), ticket);
                parkedPlates.add(vehicle.plate());
                return Optional.of(ticket);
            }
        }
        return Optional.empty(); // lot full for this vehicle type
    }

    synchronized Receipt unpark(long ticketId, LocalDateTime now) {
        Ticket ticket = activeTickets.remove(ticketId);
        if (ticket == null) throw new IllegalArgumentException("invalid or already used ticket " + ticketId);
        ParkingSpot spot = spotsById.get(ticket.spotId());
        spot.vehicle = null;
        freeSpots.get(spot.size).add(spot);
        parkedPlates.remove(ticket.vehicle().plate());
        return new Receipt(ticket, now, pricing.feeCents(ticket.vehicle().type(), Duration.between(ticket.entryTime(), now)));
    }

    synchronized Map<SpotSize, Integer> availability() {
        Map<SpotSize, Integer> m = new EnumMap<>(SpotSize.class);
        freeSpots.forEach((size, q) -> m.put(size, q.size()));
        return m;
    }

    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot(1, 1, 2, 1, new HourlyPricing());
        LocalDateTime t0 = LocalDateTime.of(2026, 1, 15, 9, 0);
        System.out.println("free spots: " + lot.availability());

        Ticket moto = lot.park(new Vehicle("MOTO-1", VehicleType.MOTORCYCLE), t0).orElseThrow();
        Ticket car1 = lot.park(new Vehicle("CAR-1", VehicleType.CAR), t0).orElseThrow();
        Ticket car2 = lot.park(new Vehicle("CAR-2", VehicleType.CAR), t0.plusMinutes(5)).orElseThrow();
        Ticket car3 = lot.park(new Vehicle("CAR-3", VehicleType.CAR), t0.plusMinutes(10)).orElseThrow(); // mediums full -> LARGE
        System.out.println("parked: " + List.of(moto.spotId(), car1.spotId(), car2.spotId(), car3.spotId()));
        System.out.println("truck parks? " + lot.park(new Vehicle("TRUCK-1", VehicleType.TRUCK), t0.plusMinutes(15))
                .map(Ticket::spotId).orElse("no - lot full for trucks"));
        System.out.println("free spots: " + lot.availability());

        System.out.println(lot.unpark(car3.id(), t0.plusMinutes(150)));   // 140 min -> rounds up to 3 h * $3 = $9.00
        System.out.println(lot.unpark(moto.id(), t0.plusMinutes(20)));    // 20 min -> minimum 1 h * $1 = $1.00
        System.out.println("truck parks now? " + lot.park(new Vehicle("TRUCK-1", VehicleType.TRUCK), t0.plusMinutes(160))
                .map(Ticket::spotId).orElse("no"));                       // the LARGE spot car3 used is free again
        try {
            lot.unpark(car3.id(), t0.plusMinutes(200));
        } catch (IllegalArgumentException e) {
            System.out.println("reuse ticket -> " + e.getMessage());
        }
        System.out.println("free spots: " + lot.availability());
    }
}
