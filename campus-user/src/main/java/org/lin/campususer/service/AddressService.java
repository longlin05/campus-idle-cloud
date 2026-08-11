package org.lin.campususer.service;

import org.lin.common.entity.Address;

import java.util.List;

public interface AddressService {
    List<Address> getAddressesByUserId(Long userId);
    Address getDefaultAddress(Long userId);
    Address getAddressById(Long userId, Long addressId);
    Address addAddress(Long userId, Address address);
    Address updateAddress(Long userId, Long addressId, Address address);
    void deleteAddress(Long userId, Long addressId);
    void setDefaultAddress(Long userId, Long addressId);
}