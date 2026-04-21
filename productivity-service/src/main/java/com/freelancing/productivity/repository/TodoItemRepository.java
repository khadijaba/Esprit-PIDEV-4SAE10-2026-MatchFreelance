package com.freelancing.productivity.repository;

import com.freelancing.productivity.entity.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TodoItemRepository extends JpaRepository<TodoItem, Long>, JpaSpecificationExecutor<TodoItem> {
    List<TodoItem> findByListIdOrderByPositionIndexAscIdAsc(Long listId);

    long countByListIdAndDoneTrue(Long listId);

    long countByListId(Long listId);
}
