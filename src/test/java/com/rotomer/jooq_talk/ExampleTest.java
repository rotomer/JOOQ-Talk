package com.rotomer.jooq_talk;

import com.rotomer.jooq_talk.model.db.tables.records.AddressRecord;
import com.rotomer.jooq_talk.model.db.tables.records.CustomerRecord;
import org.immutables.value.Value;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static com.rotomer.jooq_talk.model.db.tables.Address.ADDRESS;
import static com.rotomer.jooq_talk.model.db.tables.Customer.CUSTOMER;
import static org.jooq.impl.DSL.count;

public class ExampleTest {

    private static final SQLDialect DIALECT = SQLDialect.SQLSERVER2017;
    private static final Logger _logger = LoggerFactory.getLogger(ExampleTest.class);

    private static CustomerRecord _lastCustomer;

    @BeforeClass
    public static void setUp() throws SQLException {
        _logger.info("Starting to fill DB with test data...");
        try (final var connection = createSqlConnection()) {
            for (var i = 0; i < 100; i++) {
                final var customer = addCustomer(i, connection);

                addAddress((i * 10) + 1, customer, connection);
                final var billingAddress = addAddress((i * 10) + 2, customer, connection);

                setBillingAddress(customer.getId(), billingAddress.getId(), connection);

                _lastCustomer = customer;
            }

            addTelAvivAddress(_lastCustomer, connection);
        }

        _logger.info("Finished filling DB with test data.");
    }

    @Test
    public void fetchOneShortVersion() throws SQLException {
        try (final var connection = createSqlConnection()) {
            final var db = DSL.using(connection, DIALECT);
            final var customerId = _lastCustomer.getId();

            final var customerRecord =
                    db.fetchOne(CUSTOMER, CUSTOMER.ID.eq(customerId));

            _logger.info("fetched record:\n" + customerRecord);
        }
    }

    @Test
    public void fetchOneLongVersion() throws SQLException {
        try (final var connection = createSqlConnection()) {
            final var db = DSL.using(connection, DIALECT);
            final var customerId = _lastCustomer.getId();

            final var customerRecord =
                    db.selectFrom(CUSTOMER)
                            .where(CUSTOMER.ID.eq(customerId))
                            .fetchOne();

            _logger.info("fetched record:\n" + customerRecord);
        }
    }

    @Test
    public void fetchMany() throws SQLException {
        try (final var connection = createSqlConnection()) {
            final var db = DSL.using(connection, DIALECT);

            final var mailAddress = ADDRESS.as("mailAddress");
            final var billingAddress = ADDRESS.as("billingAddress");

            final var result =
                    db.select(CUSTOMER.ID)
                            .from(CUSTOMER
                                    .join(mailAddress).on(CUSTOMER.ID.eq(mailAddress.CUSTOMER_ID))
                                    .join(billingAddress).on(CUSTOMER.BILLING_ADDRESS_ID.eq(billingAddress.ID)))
                            .where(billingAddress.CITY.eq("Tel Aviv"))
                            .groupBy(CUSTOMER.ID)
                            .having(count().greaterThan(1))
                            .fetchMany();

            _logger.info("fetched records:\n" + result);
        }
    }

    @Test
    public void getGeneratedSql() throws SQLException {
        try (final var connection = createSqlConnection()) {
            final var db = DSL.using(connection, DIALECT);

            final var mailAddress = ADDRESS.as("mailAddress");
            final var billingAddress = ADDRESS.as("billingAddress");

            final var sql =
                    db.select(CUSTOMER.ID)
                            .from(CUSTOMER
                                    .join(mailAddress).on(CUSTOMER.ID.eq(mailAddress.CUSTOMER_ID))
                                    .join(billingAddress).on(CUSTOMER.BILLING_ADDRESS_ID.eq(billingAddress.ID)))
                            .where(billingAddress.CITY.eq("Tel Aviv"))
                            .groupBy(CUSTOMER.ID)
                            .having(count().greaterThan(1))
                            .getSQL();

            _logger.info("generated sql:\n" + sql);
        }
    }

    @Test
    public void typedProjection() throws SQLException {
        try (final var connection = createSqlConnection()) {
            final var db = DSL.using(connection, DIALECT);

            final var customerId = _lastCustomer.getId();

            final Record record =
                    db.select()
                            .from(CUSTOMER.join(ADDRESS).on(CUSTOMER.BILLING_ADDRESS_ID.eq(ADDRESS.ID)))
                            .where(CUSTOMER.ID.eq(customerId))
                            .fetchOne();

            final CustomerRecord customer = record.into(CUSTOMER);
            final AddressRecord address = record.into(ADDRESS);

            _logger.info("fetched customer:\n" + customer);
            _logger.info("fetched address:\n" + address);
        }
    }

    @Test
    public void mapToDto() throws SQLException {
        try (final var connection = createSqlConnection()) {
            final var db = DSL.using(connection, DIALECT);

            final var customerId = _lastCustomer.getId();

            final Record record =
                    db.select(CUSTOMER.ID, CUSTOMER.NAME, ADDRESS.CITY, ADDRESS.COUNTRY)
                            .from(CUSTOMER.join(ADDRESS).on(CUSTOMER.BILLING_ADDRESS_ID.eq(ADDRESS.ID)))
                            .where(CUSTOMER.ID.eq(customerId))
                            .fetchOne();

            final CustomerDto customerDto = record.into(ImmutableCustomerDto.class);

            _logger.info("fetched customer DTO:\n" + customerDto);
        }
    }

    /**
     * Note that usage of a connection pooling library like HikariCP is advised for non-toy projects.
     */
    private static Connection createSqlConnection() throws SQLException {
        final var jooqTalkDbUrl = System.getenv("DB_URL") + ";databaseName=JOOQ_TALK";
        final var dbUser = System.getenv("SQL_DB_USER");
        final var dbPassword = System.getenv("SQL_DB_PASSWORD");

        return DriverManager.getConnection(jooqTalkDbUrl, dbUser, dbPassword);
    }

    private static CustomerRecord addCustomer(final int serial,
                                              final Connection connection) {
        final var db = DSL.using(connection, DIALECT);

        final var customerRecord = db.newRecord(CUSTOMER);

        customerRecord.setName("name_" + serial);
        customerRecord.setEmail("mail_" + serial + "@gmail.com");

        customerRecord.store();

        return customerRecord;
    }

    private static AddressRecord addAddress(final int serial,
                                            final CustomerRecord customerRecord,
                                            final Connection connection) {
        final var db = DSL.using(connection, DIALECT);

        final var addressRecord = db.newRecord(ADDRESS);

        addressRecord.setCustomerId(customerRecord.getId());
        addressRecord.setAddressLine_1("addressLine_1_" + serial);
        addressRecord.setAddressLine_1("addressLine_2_" + serial);
        addressRecord.setCity("city_" + serial);
        addressRecord.setState("state_" + serial);
        addressRecord.setCountry("country_" + serial);
        addressRecord.setZip("zip_" + serial);

        addressRecord.store();

        return addressRecord;
    }

    private static void addTelAvivAddress(final CustomerRecord customerRecord,
                                          final Connection connection) throws SQLException {
        final var db = DSL.using(connection, DIALECT);

        final var addressRecord = db.newRecord(ADDRESS);

        addressRecord.setCustomerId(customerRecord.getId());
        addressRecord.setAddressLine_1("Ehad Haam 1");
        addressRecord.setCity("Tel Aviv");
        addressRecord.setCountry("Israel");
        addressRecord.setZip("6706060");

        addressRecord.store();

        setBillingAddress(customerRecord.getId(), addressRecord.getId(), connection);
    }

    private static void setBillingAddress(final long customerId,
                                          final long addressId,
                                          final Connection connection) throws SQLException {
        final var db = DSL.using(connection, DIALECT);

        db.update(CUSTOMER)
                .set(CUSTOMER.BILLING_ADDRESS_ID, addressId)
                .where(CUSTOMER.ID.eq(customerId))
                .execute();

        connection.commit();
    }

    @Value.Immutable
    public static abstract class CustomerDto {
        @Value.Parameter public abstract long customerId();
        @Value.Parameter public abstract String name();
        @Value.Parameter public abstract String city();
        @Value.Parameter public abstract String country();
    }
}