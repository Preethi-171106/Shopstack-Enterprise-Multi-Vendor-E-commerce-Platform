package com.shopstack.notification.exception;

import com.shopstack.common.exception.ResourceNotFoundException;

public class NotificationNotFoundException extends ResourceNotFoundException {

    public NotificationNotFoundException(String message) {
        super(message);
    }

    public NotificationNotFoundException(Object id) {
        super("Notification not found with id: " + id);
    }
}
