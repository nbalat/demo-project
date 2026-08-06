package com.example.demo.repository

import com.example.demo.model.OrderEntity
import com.example.demo.model.OrderItemEntity
import com.example.demo.model.ProductEntity
import com.example.demo.model.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByEmail(email: String): UserEntity?
}

@Repository
interface ProductRepository : JpaRepository<ProductEntity, Long> {
    fun findByCategory(category: String): List<ProductEntity>
    fun findAllByOrderByNameAsc(): List<ProductEntity>
}

@Repository
interface OrderRepository : JpaRepository<OrderEntity, Long> {
    fun findByUserEmailOrderByTimestampDesc(userEmail: String): List<OrderEntity>
    fun findAllByOrderByTimestampDesc(): List<OrderEntity>
    fun findByOrderId(orderId: String): OrderEntity?
}

@Repository
interface OrderItemRepository : JpaRepository<OrderItemEntity, Long> {
    fun findByOrderId(orderId: String): List<OrderItemEntity>
}
