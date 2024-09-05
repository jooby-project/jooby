package tests.validation;

import io.jooby.Context;

class Bean {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static Bean map(Context ctx) {
        return ctx.body(Bean.class);
    }
}
