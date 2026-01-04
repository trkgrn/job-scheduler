package com.trkgrn.jobscheduler.platform.common.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

@Component
public class Localization {

    private static MessageSource messageSource;

    public Localization(MessageSource messageSource) {
        Localization.messageSource = messageSource;
    }

    public static String getLocalizedMessage(String key) {
        Locale locale = LocaleContextHolder.getLocale();
        return getLocalizedMessage(key, locale);
    }

    public static String getLocalizedMessage(String key, Locale locale) {
        if (Objects.isNull(locale)) {
            locale = Locale.ENGLISH;
        }
        try {
            return messageSource.getMessage(key, null, locale);
        } catch (Exception e) {
            return key;
        }
    }

    public static String getLocalizedMessage(String key, Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        if (Objects.isNull(locale)) {
            locale = Locale.ENGLISH;
        }
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (Exception e) {
            return key;
        }
    }
}

