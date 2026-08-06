package com.example.demo.model

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var email: String = "",

    @Column(nullable = false)
    var password: String = "",

    @Column(nullable = false)
    var fullName: String = "",

    @Column(nullable = false)
    var phone: String = "",

    @Column(nullable = false)
    var role: String = "ROLE_USER" // ROLE_ADMIN or ROLE_USER
)

@Entity
@Table(name = "products")
data class ProductEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false)
    var category: String = "Grains", // Grains, Pulses, Millets, Spices

    @Column(length = 1000)
    var description: String = "",

    @Column(nullable = false)
    var price: Double = 0.0,

    @Column(nullable = false)
    var unit: String = "kg", // kg, gm, pack

    @Column(length = 1000)
    var imageUrl: String = "",

    @Column(nullable = false)
    var stockQuantity: Double = 100.0
)

@Entity
@Table(name = "orders")
data class OrderEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var orderId: String = "",

    @Column(nullable = false)
    var userEmail: String = "",

    @Column(nullable = false)
    var customerName: String = "",

    @Column(nullable = false)
    var customerPhone: String = "",

    @Column(length = 1000)
    var deliveryAddress: String = "",

    @Column(nullable = false)
    var paymentMode: String = "COD", // COD, UPI, CARD

    @Column(nullable = false)
    var totalAmount: Double = 0.0,

    @Column(nullable = false)
    var timestamp: String = "",

    @Column(nullable = false)
    var status: String = "CONFIRMED"
)

@Entity
@Table(name = "order_items")
data class OrderItemEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var orderId: String = "",

    @Column(nullable = false)
    var productName: String = "",

    @Column(nullable = false)
    var unitPrice: Double = 0.0,

    @Column(nullable = false)
    var quantity: Double = 1.0,

    @Column(nullable = false)
    var unit: String = "kg",

    @Column(nullable = false)
    var subtotal: Double = 0.0
)
