package com.example.expenseclaim.service;

import com.example.expenseclaim.dto.EmployeeRequest;
import com.example.expenseclaim.exception.InvalidStateException;
import com.example.expenseclaim.exception.ResourceNotFoundException;
import com.example.expenseclaim.model.Employee;
import com.example.expenseclaim.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public Employee createEmployee(EmployeeRequest request) {
        // Check for duplicate email
        employeeRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            throw new InvalidStateException("An employee with email '" + request.getEmail() + "' already exists.");
        });

        Employee employee = new Employee();
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setDepartment(request.getDepartment());
        employee.setManagerName(request.getManagerName());

        return employeeRepository.save(employee);
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    public void deleteEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
    }
}
