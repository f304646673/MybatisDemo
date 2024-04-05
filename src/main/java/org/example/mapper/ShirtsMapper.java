package org.example.mapper;

import org.example.model.Shirts;

import java.util.List;

public interface ShirtsMapper {

    List<Shirts> findShirts(Shirts.ShirtSize size);

    long updateShirts(Shirts item);
}
