package com.example.demo.config

import com.example.demo.model.ProductEntity
import com.example.demo.model.UserEntity
import com.example.demo.repository.ProductRepository
import com.example.demo.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataSeeder(
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        seedUsers()
        seedProducts()
    }

    private fun seedUsers() {
        if (userRepository.count() > 0) return

        val adminPassword = passwordEncoder.encode("admin123")
        val userPassword = passwordEncoder.encode("user123")

        // 1. Seed 3 Admin Accounts
        val admins = listOf(
            UserEntity(email = "admin1@organic.com", password = adminPassword, fullName = "Master Admin One", phone = "9876543210", role = "ROLE_ADMIN"),
            UserEntity(email = "admin2@organic.com", password = adminPassword, fullName = "Store Manager Admin Two", phone = "9876543211", role = "ROLE_ADMIN"),
            UserEntity(email = "admin3@organic.com", password = adminPassword, fullName = "Inventory Admin Three", phone = "9876543212", role = "ROLE_ADMIN")
        )

        // 2. Seed 5 Customer Accounts
        val customers = listOf(
            UserEntity(email = "user1@organic.com", password = userPassword, fullName = "Niraj ", phone = "9876543220", role = "ROLE_USER"),
            UserEntity(email = "user2@organic.com", password = userPassword, fullName = "Priya Sharma", phone = "9876543221", role = "ROLE_USER"),
            UserEntity(email = "user3@organic.com", password = userPassword, fullName = "Amit Kumar", phone = "9876543222", role = "ROLE_USER"),
            UserEntity(email = "user4@organic.com", password = userPassword, fullName = "Sanjay Verma", phone = "9876543223", role = "ROLE_USER"),
            UserEntity(email = "user5@organic.com", password = userPassword, fullName = "Neha Gupta", phone = "9876543224", role = "ROLE_USER")
        )

        userRepository.saveAll(admins)
        userRepository.saveAll(customers)
        println("✅ DataSeeder: Seeded 3 Admin accounts and 5 Customer accounts into SQLite DB.")
    }

    private fun seedProducts() {
        if (productRepository.count() > 0) return

        val initialProducts = listOf(
            ProductEntity(
                name = "Organic Kabuli Chana (Chickpeas)",
                category = "Pulses",
                description = "Handpicked organic white chickpeas, high protein, 100% natural without polishing.",
                price = 140.0,
                unit = "kg",
                imageUrl = "https://images.unsplash.com/photo-1515543904379-3d757afe72e0?w=600&auto=format&fit=crop",
                stockQuantity = 250.0
            ),
            ProductEntity(
                name = "Premium Desi Val (Field Beans)",
                category = "Pulses",
                description = "Traditional Gujarati Desi Val, rich fiber, nutrient-packed organic beans.",
                price = 160.0,
                unit = "kg",
                imageUrl = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600&auto=format&fit=crop",
                stockQuantity = 180.0
            ),
            ProductEntity(
                name = "Organic White Vatana (Dried Peas)",
                category = "Pulses",
                description = "Naturally sun-dried white vatana, perfect for ragda and traditional curries.",
                price = 95.0,
                unit = "kg",
                imageUrl = "https://images.unsplash.com/photo-1584270354949-c26b0d5b4a0c?w=600&auto=format&fit=crop",
                stockQuantity = 300.0
            ),
            ProductEntity(
                name = "Organic Green Vatana (Dried Green Peas)",
                category = "Pulses",
                description = "Pure organic green vatana, pesticide-free, unpolished premium quality.",
                price = 110.0,
                unit = "kg",
                imageUrl = "https://images.unsplash.com/photo-1592417817098-8f3d6eb19655?w=600&auto=format&fit=crop",
                stockQuantity = 200.0
            ),
            ProductEntity(
                name = "Organic Foxtail Millet (Kangni)",
                category = "Millets",
                description = "Gluten-free diabetic-friendly organic foxtail millet, high calcium & protein.",
                price = 130.0,
                unit = "kg",
                imageUrl = "https://images.unsplash.com/photo-1586201375761-83865001e8ac?w=600&auto=format&fit=crop",
                stockQuantity = 150.0
            ),
            ProductEntity(
                name = "Organic Pearl Millet (Bajra Grain)",
                category = "Millets",
                description = "Traditional organic Bajra grains, perfect for winter bhakri and rotla.",
                price = 65.0,
                unit = "kg",
                imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=600&auto=format&fit=crop",
                stockQuantity = 400.0
            ),
            ProductEntity(
                name = "Sharbati Whole Wheat Grain",
                category = "Grains",
                description = "Golden Sharbati farm wheat grains, unpolished, stone-ground flour quality.",
                price = 55.0,
                unit = "kg",
                imageUrl = "https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?w=600&auto=format&fit=crop",
                stockQuantity = 500.0
            ),
            ProductEntity(
                name = "Royal Organic Basmati Rice",
                category = "Grains",
                description = "Long-grain aromatic organic basmati rice, aged 2 years for optimal aroma.",
                price = 185.0,
                unit = "kg",
                imageUrl = "https://images.unsplash.com/photo-1586201375761-83865001e8ac?w=600&auto=format&fit=crop",
                stockQuantity = 350.0
            ),
            ProductEntity(
                name = "Organic Unpolished Toor Dal",
                category = "Pulses",
                description = "Chemical-free unpolished Arhar/Toor dal, naturally processed.",
                price = 170.0,
                unit = "kg",
                imageUrl = "https://images.unsplash.com/photo-1515543904379-3d757afe72e0?w=600&auto=format&fit=crop",
                stockQuantity = 220.0
            ),
            ProductEntity(
                name = "Pure Organic Lakadong Turmeric Powder",
                category = "Spices",
                description = "High-curcumin (7%+) organic turmeric powder from Meghalaya farms.",
                price = 280.0,
                unit = "pack",
                imageUrl = "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=600&auto=format&fit=crop",
                stockQuantity = 100.0
            )
        )

        productRepository.saveAll(initialProducts)
        println("✅ DataSeeder: Seeded 10 Organic Food products into SQLite DB.")
    }
}
