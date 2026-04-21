package com.freelancing.productivity.service;

import com.freelancing.productivity.dto.*;
import com.freelancing.productivity.entity.TodoItem;
import com.freelancing.productivity.entity.TodoList;
import com.freelancing.productivity.repository.TodoItemRepository;
import com.freelancing.productivity.repository.TodoListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoListRepository listRepository;
    private final TodoItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<TodoListResponseDTO> getListsByOwner(Long ownerId) {
        return listRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream().map(this::toListDto).toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<TodoListResponseDTO> getListsByOwnerPaged(Long ownerId, String query, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("createdAt"))
        );

        Specification<TodoList> spec = (root, q, cb) -> cb.equal(root.get("ownerId"), ownerId);
        if (query != null && !query.isBlank()) {
            String normalized = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), normalized));
        }

        Page<TodoListResponseDTO> mapped = listRepository.findAll(spec, pageRequest).map(this::toListDto);
        return PageResponseDTO.from(mapped);
    }

    @Transactional
    public TodoListResponseDTO createList(Long ownerId, TodoListCreateRequestDTO request) {
        TodoList list = new TodoList();
        list.setOwnerId(ownerId);
        list.setName(request.getName().trim());
        return toListDto(listRepository.save(list));
    }

    @Transactional
    public TodoListResponseDTO renameList(Long listId, TodoListCreateRequestDTO request) {
        TodoList list = findList(listId);
        list.setName(request.getName().trim());
        return toListDto(listRepository.save(list));
    }

    @Transactional
    public void deleteList(Long listId) {
        if (!listRepository.existsById(listId)) {
            throw new IllegalArgumentException("Todo list not found with id: " + listId);
        }
        itemRepository.findByListIdOrderByPositionIndexAscIdAsc(listId)
                .forEach(item -> itemRepository.deleteById(item.getId()));
        listRepository.deleteById(listId);
    }

    @Transactional(readOnly = true)
    public List<TodoItemResponseDTO> getItems(Long listId) {
        return itemRepository.findByListIdOrderByPositionIndexAscIdAsc(listId)
                .stream().map(this::toItemDto).toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<TodoItemResponseDTO> getItemsPaged(
            Long listId,
            String query,
            Boolean done,
            Instant dueFrom,
            Instant dueTo,
            int page,
            int size
    ) {
        findList(listId);

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.asc("positionIndex"), Sort.Order.asc("id"))
        );

        Specification<TodoItem> spec = (root, q, cb) -> cb.equal(root.get("listId"), listId);
        if (query != null && !query.isBlank()) {
            String normalized = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("title")), normalized));
        }
        if (done != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("done"), done));
        }
        if (dueFrom != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("dueAt"), dueFrom));
        }
        if (dueTo != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("dueAt"), dueTo));
        }

        Page<TodoItemResponseDTO> mapped = itemRepository.findAll(spec, pageRequest).map(this::toItemDto);
        return PageResponseDTO.from(mapped);
    }

    @Transactional
    public TodoItemResponseDTO createItem(Long listId, TodoItemCreateRequestDTO request) {
        TodoList list = findList(listId);
        TodoItem item = new TodoItem();
        item.setListId(listId);
        item.setOwnerId(list.getOwnerId());
        item.setTitle(request.getTitle().trim());
        item.setDueAt(request.getDueAt());
        item.setDone(false);
        item.setPositionIndex(itemRepository.findByListIdOrderByPositionIndexAscIdAsc(listId).size());
        return toItemDto(itemRepository.save(item));
    }

    @Transactional
    public TodoItemResponseDTO updateItem(Long itemId, TodoItemUpdateRequestDTO request) {
        TodoItem item = findItem(itemId);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            item.setTitle(request.getTitle().trim());
        }
        if (request.getDone() != null) {
            item.setDone(request.getDone());
        }
        if (request.getDueAt() != null) {
            item.setDueAt(request.getDueAt());
        }
        if (request.getPositionIndex() != null) {
            item.setPositionIndex(request.getPositionIndex());
        }
        return toItemDto(itemRepository.save(item));
    }

    @Transactional
    public TodoItemResponseDTO toggleItem(Long itemId) {
        TodoItem item = findItem(itemId);
        item.setDone(!item.isDone());
        return toItemDto(itemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new IllegalArgumentException("Todo item not found with id: " + itemId);
        }
        itemRepository.deleteById(itemId);
    }

    private TodoList findList(Long listId) {
        return listRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Todo list not found with id: " + listId));
    }

    private TodoItem findItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Todo item not found with id: " + itemId));
    }

    private TodoListResponseDTO toListDto(TodoList list) {
        TodoListResponseDTO dto = new TodoListResponseDTO();
        dto.setId(list.getId());
        dto.setOwnerId(list.getOwnerId());
        dto.setName(list.getName());
        dto.setCreatedAt(list.getCreatedAt());
        dto.setTotalItems(itemRepository.countByListId(list.getId()));
        dto.setCompletedItems(itemRepository.countByListIdAndDoneTrue(list.getId()));
        return dto;
    }

    private TodoItemResponseDTO toItemDto(TodoItem item) {
        TodoItemResponseDTO dto = new TodoItemResponseDTO();
        dto.setId(item.getId());
        dto.setOwnerId(item.getOwnerId());
        dto.setListId(item.getListId());
        dto.setTitle(item.getTitle());
        dto.setDone(item.isDone());
        dto.setPositionIndex(item.getPositionIndex());
        dto.setDueAt(item.getDueAt());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }
}

