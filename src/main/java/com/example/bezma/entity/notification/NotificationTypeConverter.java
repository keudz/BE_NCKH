package com.example.bezma.entity.notification;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {

    @Override
    public String convertToDatabaseColumn(NotificationType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public NotificationType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return NotificationType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // Log warning or fallback to GENERAL instead of crashing
            System.err.println("Unrecognized NotificationType in DB: " + dbData + ". Falling back to GENERAL.");
            return NotificationType.GENERAL;
        }
    }
}
