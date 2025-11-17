package com.specifications;

import com.entities.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> firstNameContains(String name) {

        return (root, query, cb) ->
                name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");

    }

    public static Specification<User> surnameContains(String surname) {

        return (root, query, cb) ->
                surname == null ? null : cb.like(cb.lower(root.get("surname")), "%" + surname.toLowerCase() + "%");

    }
}
