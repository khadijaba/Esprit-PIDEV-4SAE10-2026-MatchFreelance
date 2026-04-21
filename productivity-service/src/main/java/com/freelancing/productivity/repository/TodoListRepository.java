package com.freelancing.productivity.repository;

import com.freelancing.productivity.entity.TodoList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TodoListRepository extends JpaRepository<TodoList, Long>, JpaSpecificationExecutor<TodoList> {
    List<TodoList> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
