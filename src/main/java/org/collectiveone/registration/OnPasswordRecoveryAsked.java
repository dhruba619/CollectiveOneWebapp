package org.collectiveone.registration;

import java.util.Locale;

import org.collectiveone.model.User;
import org.springframework.context.ApplicationEvent;

@SuppressWarnings("serial")
public class OnPasswordRecoveryAsked extends ApplicationEvent {

    private final String appUrl;
    private final Locale locale;
    private final User user;

    public OnPasswordRecoveryAsked(final User user, final Locale locale, final String appUrl) {
        super(user);
        this.user = user;
        this.locale = locale;
        this.appUrl = appUrl;
    }

    //

    public String getAppUrl() {
        return appUrl;
    }

    public Locale getLocale() {
        return locale;
    }

    public User getUser() {
        return user;
    }

}
