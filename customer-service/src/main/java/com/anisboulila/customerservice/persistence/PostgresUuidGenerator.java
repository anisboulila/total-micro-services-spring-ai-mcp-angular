package com.anisboulila.customerservice.persistence;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;

import java.lang.reflect.Member;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.UUID;

public class PostgresUuidGenerator implements BeforeExecutionGenerator {

    public PostgresUuidGenerator(
            PostgresGeneratedUuid annotation,
            Member member,
            CustomIdGeneratorCreationContext context) {
    }

    @Override
    public Object generate(
            SharedSessionContractImplementor session,
            Object owner,
            Object currentValue,
            EventType eventType) {
        return session.doReturningWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT gen_random_uuid()");
                 ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("PostgreSQL did not return a generated UUID");
                }
                return resultSet.getObject(1, UUID.class);
            }
        });
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }
}
