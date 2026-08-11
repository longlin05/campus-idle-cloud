package org.lin.campususer.controller;

import org.lin.common.jwt.JwtAuth;
import org.lin.common.result.Result;
import org.lin.common.threadlocal.UserThreadLocal;
import org.lin.common.entity.Address;
import org.lin.campususer.service.AddressService;
import org.lin.campususer.vo.AddressVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @JwtAuth
    @GetMapping
    public Result<List<AddressVO>> getAddresses() {
        Long userId = UserThreadLocal.get().getId();
        List<Address> entities = addressService.getAddressesByUserId(userId);
        List<AddressVO> vos = entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    @JwtAuth
    @GetMapping("/default")
    public Result<AddressVO> getDefaultAddress() {
        Long userId = UserThreadLocal.get().getId();
        Address entity = addressService.getDefaultAddress(userId);
        return Result.success(entity != null ? toVO(entity) : null);
    }

    @JwtAuth
    @GetMapping("/{addressId}")
    public Result<AddressVO> getAddress(@PathVariable Long addressId) {
        Long userId = UserThreadLocal.get().getId();
        Address entity = addressService.getAddressById(userId, addressId);
        return Result.success(entity != null ? toVO(entity) : null);
    }

    @JwtAuth
    @PostMapping
    public Result<AddressVO> addAddress(@RequestBody AddressVO vo) {
        Long userId = UserThreadLocal.get().getId();
        Address entity = toEntity(vo);
        Address saved = addressService.addAddress(userId, entity);
        return Result.success(toVO(saved));
    }

    @JwtAuth
    @PutMapping("/{addressId}")
    public Result<AddressVO> updateAddress(@PathVariable Long addressId, @RequestBody AddressVO vo) {
        Long userId = UserThreadLocal.get().getId();
        Address entity = toEntity(vo);
        entity.setAddressId(addressId);
        Address updated = addressService.updateAddress(userId, addressId, entity);
        return Result.success(updated != null ? toVO(updated) : null);
    }

    @JwtAuth
    @DeleteMapping("/{addressId}")
    public Result<Void> deleteAddress(@PathVariable Long addressId) {
        Long userId = UserThreadLocal.get().getId();
        addressService.deleteAddress(userId, addressId);
        return Result.success();
    }

    @JwtAuth
    @PutMapping("/{addressId}/default")
    public Result<Void> setDefaultAddress(@PathVariable Long addressId) {
        Long userId = UserThreadLocal.get().getId();
        addressService.setDefaultAddress(userId, addressId);
        return Result.success();
    }

    // ==================== VO <-> Entity 转换 ====================

    private AddressVO toVO(Address entity) {
        if (entity == null) return null;
        AddressVO vo = new AddressVO();
        vo.setId(entity.getAddressId());
        vo.setReceiverName(entity.getReceiverName());
        vo.setReceiverPhone(entity.getReceiverPhone());
        // 拼接完整地址
        StringBuilder sb = new StringBuilder();
        if (entity.getProvince() != null) sb.append(entity.getProvince());
        if (entity.getCity() != null) sb.append(entity.getCity());
        if (entity.getDistrict() != null) sb.append(entity.getDistrict());
        if (entity.getDetailAddress() != null) sb.append(entity.getDetailAddress());
        vo.setReceiverAddress(sb.toString());
        vo.setProvince(entity.getProvince());
        vo.setCity(entity.getCity());
        vo.setDistrict(entity.getDistrict());
        vo.setDetailAddress(entity.getDetailAddress());
        vo.setIsDefault(entity.getIsDefault() != null ? entity.getIsDefault() : 0);
        return vo;
    }

    private Address toEntity(AddressVO vo) {
        if (vo == null) return null;
        Address entity = new Address();
        entity.setReceiverName(vo.getReceiverName());
        entity.setReceiverPhone(vo.getReceiverPhone());
        // 兼容两种格式：优先使用分字段，否则用 receiverAddress
        if (vo.getProvince() != null || vo.getCity() != null || vo.getDetailAddress() != null) {
            entity.setProvince(vo.getProvince());
            entity.setCity(vo.getCity());
            entity.setDistrict(vo.getDistrict());
            entity.setDetailAddress(vo.getDetailAddress());
        } else {
            entity.setDetailAddress(vo.getReceiverAddress());
        }
        entity.setIsDefault(vo.getIsDefault() != null ? vo.getIsDefault() : 0);
        return entity;
    }
}
