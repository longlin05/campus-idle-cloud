package org.lin.campususer.service.impl;

import org.lin.common.exception.BusinessException;
import org.lin.common.entity.Address;
import org.lin.campususer.mapper.AddressMapper;
import org.lin.campususer.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressMapper addressMapper;

    @Override
    public List<Address> getAddressesByUserId(Long userId) {
        return addressMapper.findByUserId(userId);
    }

    @Override
    public Address getDefaultAddress(Long userId) {
        return addressMapper.findDefaultByUserId(userId);
    }

    @Override
    public Address getAddressById(Long userId, Long addressId) {
        Address address = addressMapper.selectById(addressId);
        if (address != null && !address.getUserId().equals(userId)) {
            return null;
        }
        return address;
    }

    @Override
    @Transactional
    public Address addAddress(Long userId, Address address) {
        address.setUserId(userId);
        address.setCreateTime(new Date());
        address.setUpdateTime(new Date());
        address.setIsDeleted(0);

        if (address.getIsDefault() == null) {
            address.setIsDefault(0);
        }

        if (address.getIsDefault() == 1) {
            resetDefaultAddress(userId);
        }

        addressMapper.insert(address);
        return address;
    }

    @Override
    @Transactional
    public Address updateAddress(Long userId, Long addressId, Address address) {
        Address existing = addressMapper.selectById(addressId);
        if (existing == null) {
            throw new BusinessException("地址不存在");
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此地址");
        }

        existing.setReceiverName(address.getReceiverName());
        existing.setReceiverPhone(address.getReceiverPhone());
        existing.setProvince(address.getProvince());
        existing.setCity(address.getCity());
        existing.setDistrict(address.getDistrict());
        existing.setDetailAddress(address.getDetailAddress());
        existing.setUpdateTime(new Date());

        if (address.getIsDefault() != null && address.getIsDefault() == 1 && existing.getIsDefault() != 1) {
            resetDefaultAddress(userId);
            existing.setIsDefault(1);
        }

        addressMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此地址");
        }
        addressMapper.deleteById(addressId);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException("地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("无权设置此地址");
        }

        resetDefaultAddress(userId);
        address.setIsDefault(1);
        address.setUpdateTime(new Date());
        addressMapper.updateById(address);
    }

    private void resetDefaultAddress(Long userId) {
        Address defaultAddress = addressMapper.findDefaultByUserId(userId);
        if (defaultAddress != null) {
            defaultAddress.setIsDefault(0);
            defaultAddress.setUpdateTime(new Date());
            addressMapper.updateById(defaultAddress);
        }
    }
}