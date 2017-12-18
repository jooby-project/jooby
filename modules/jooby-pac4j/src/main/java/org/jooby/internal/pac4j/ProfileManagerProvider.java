package org.jooby.internal.pac4j;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.pac4j.core.profile.ProfileManager;

public class ProfileManagerProvider implements Provider<ProfileManager> {
    AuthContext context;

    @Inject
    public ProfileManagerProvider(AuthContext context) {
        this.context = context;
    }

    @Override
    public ProfileManager get() {
        return new ProfileManager(context);
    }
}
