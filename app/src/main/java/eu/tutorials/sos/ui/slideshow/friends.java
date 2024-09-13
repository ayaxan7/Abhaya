package eu.tutorials.sos.ui.slideshow;

import com.google.firebase.auth.FirebaseUser;

public class friends {
    private String Phone;
    private String name;

    public friends() {}

    public friends(String Phone, String name) {
        this.Phone = Phone;
        this.name = name;
    }

    public String getPhone() {
        return Phone;
    }

    public void setPhone(String Phone) {
        this.Phone = Phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

