package com.mash.api.service.impl;

import com.mash.api.entity.Customer;
import com.mash.api.repository.CustomerRepository;
import com.mash.api.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public Page<Customer> findByVendorId(Pageable pageable, Integer vendorId) {
        return customerRepository.findByVendorId(pageable, vendorId);
    }

    @Override
    public Page<Customer> findByVendorIdAndEmployeeId(Pageable pageable, Integer vendorId, Integer employeeId) {
        return customerRepository.findByVendorIdAndEmployeeId(pageable, vendorId, employeeId);
    }

    @Override
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    @Override
    public Customer findById(Integer id) {
        return customerRepository.findOne(id);
    }

    @Override
    public void deleteById(Integer id) {
        customerRepository.delete(id);
    }

    @Override
    public Page<Customer> findByParams(Specification<Customer> specification, Pageable pageable) {
        return customerRepository.findAll(specification, pageable);
    }

    @Override
    public Customer findByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }
}
