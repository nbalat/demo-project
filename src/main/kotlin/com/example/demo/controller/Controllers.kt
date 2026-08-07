package com.example.demo.controller

import com.example.demo.model.OrderEntity
import com.example.demo.model.OrderItemEntity
import com.example.demo.model.ProductEntity
import com.example.demo.model.UserEntity
import com.example.demo.repository.OrderItemRepository
import com.example.demo.repository.OrderRepository
import com.example.demo.repository.ProductRepository
import com.example.demo.repository.UserRepository
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import java.awt.Color as AwtColor
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.Principal
import java.text.SimpleDateFormat
import java.util.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Controller
class AuthController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    @GetMapping("/login")
    fun loginPage(@RequestParam(name = "error", required = false) error: Boolean?, model: Model): String {
        if (error == true) {
            model.addAttribute("errorMessage", "Invalid Email or Password. Please try again.")
        }
        return "login"
    }

    @GetMapping("/register")
    fun registerPage(): String {
        return "register"
    }

    @PostMapping("/register")
    fun handleRegister(
        @RequestParam("email") email: String,
        @RequestParam("password") password: String,
        @RequestParam("fullName") fullName: String,
        @RequestParam("phone") phone: String,
        model: Model
    ): String {
        val cleanEmail = email.lowercase().trim()
        if (userRepository.findByEmail(cleanEmail) != null) {
            model.addAttribute("errorMessage", "Email is already registered!")
            return "register"
        }

        val newUser = UserEntity(
            email = cleanEmail,
            password = passwordEncoder.encode(password),
            fullName = fullName.trim(),
            phone = phone.trim(),
            role = "ROLE_USER"
        )
        userRepository.save(newUser)
        return "redirect:/login?registered=true"
    }
}

@Controller
class StorefrontController(
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository
) {
    @GetMapping("/", "/catalog")
    fun catalogPage(
        @RequestParam(name = "category", required = false, defaultValue = "ALL") category: String,
        @RequestParam(name = "search", required = false, defaultValue = "") search: String,
        principal: Principal?,
        model: Model
    ): String {
        val allProducts = productRepository.findAllByOrderByNameAsc()

        val filteredProducts = allProducts.filter { prod ->
            val matchesCat = if (category == "ALL") true else prod.category.equals(category, ignoreCase = true)
            val matchesSearch = if (search.isBlank()) true else prod.name.contains(search, ignoreCase = true) || prod.description.contains(search, ignoreCase = true)
            matchesCat && matchesSearch
        }

        val currentUser = if (principal != null) userRepository.findByEmail(principal.name) else null

        model.addAttribute("products", filteredProducts)
        model.addAttribute("selectedCategory", category)
        model.addAttribute("searchQuery", search)
        model.addAttribute("currentUser", currentUser)
        return "catalog"
    }
}

@Controller
class CartController(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val userRepository: UserRepository
) {
    @Suppress("UNCHECKED_CAST")
    private fun getCart(session: HttpSession): MutableMap<Long, Double> {
        var cart = session.getAttribute("SESSION_CART") as? MutableMap<Long, Double>
        if (cart == null) {
            cart = mutableMapOf()
            session.setAttribute("SESSION_CART", cart)
        }
        return cart
    }

    @GetMapping("/cart")
    fun viewCart(session: HttpSession, principal: Principal?, model: Model): String {
        val cart = getCart(session)
        val cartItems = mutableListOf<Map<String, Any>>()
        var totalAmount = 0.0

        for ((prodId, qty) in cart) {
            val prod = productRepository.findById(prodId).orElse(null) ?: continue
            val subtotal = prod.price * qty
            totalAmount += subtotal
            cartItems.add(
                mapOf(
                    "product" to prod,
                    "quantity" to qty,
                    "subtotal" to subtotal
                )
            )
        }

        val currentUser = if (principal != null) userRepository.findByEmail(principal.name) else null

        model.addAttribute("cartItems", cartItems)
        model.addAttribute("totalAmount", totalAmount)
        model.addAttribute("currentUser", currentUser)
        return "cart"
    }

    @PostMapping("/cart/add/{id}")
    fun addToCart(@PathVariable("id") id: Long, @RequestParam("quantity", defaultValue = "1.0") qty: Double, session: HttpSession): String {
        val cart = getCart(session)
        val currentQty = cart.getOrDefault(id, 0.0)
        cart[id] = currentQty + qty
        return "redirect:/cart"
    }

    @PostMapping("/cart/update/{id}")
    fun updateCart(@PathVariable("id") id: Long, @RequestParam("quantity") qty: Double, session: HttpSession): String {
        val cart = getCart(session)
        if (qty <= 0) {
            cart.remove(id)
        } else {
            cart[id] = qty
        }
        return "redirect:/cart"
    }

    @GetMapping("/cart/remove/{id}")
    fun removeFromCart(@PathVariable("id") id: Long, session: HttpSession): String {
        val cart = getCart(session)
        cart.remove(id)
        return "redirect:/cart"
    }

    @GetMapping("/checkout")
    fun checkoutPage(session: HttpSession, principal: Principal?, model: Model): String {
        val cart = getCart(session)
        if (cart.isEmpty()) return "redirect:/cart"

        var totalAmount = 0.0
        for ((prodId, qty) in cart) {
            val prod = productRepository.findById(prodId).orElse(null) ?: continue
            totalAmount += prod.price * qty
        }

        val currentUser = if (principal != null) userRepository.findByEmail(principal.name) else null

        model.addAttribute("totalAmount", totalAmount)
        model.addAttribute("currentUser", currentUser)
        return "checkout"
    }

    @PostMapping("/checkout/process")
    fun processCheckout(
        @RequestParam("customerName") customerName: String,
        @RequestParam("customerPhone") customerPhone: String,
        @RequestParam("deliveryAddress") deliveryAddress: String,
        @RequestParam("paymentMode", defaultValue = "COD") paymentMode: String,
        session: HttpSession,
        principal: Principal?
    ): String {
        val cart = getCart(session)
        if (cart.isEmpty() || principal == null) return "redirect:/catalog"

        val userEmail = principal.name
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val orderId = "ORG-${System.currentTimeMillis().toString().takeLast(7)}"

        var grandTotal = 0.0
        val itemsToSave = mutableListOf<OrderItemEntity>()

        for ((prodId, qty) in cart) {
            val prod = productRepository.findById(prodId).orElse(null) ?: continue
            val subtotal = prod.price * qty
            grandTotal += subtotal

            itemsToSave.add(
                OrderItemEntity(
                    orderId = orderId,
                    productName = prod.name,
                    unitPrice = prod.price,
                    quantity = qty,
                    unit = prod.unit,
                    subtotal = subtotal
                )
            )

            // Deduct stock quantity
            prod.stockQuantity = (prod.stockQuantity - qty).coerceAtLeast(0.0)
            productRepository.save(prod)
        }

        val order = OrderEntity(
            orderId = orderId,
            userEmail = userEmail,
            customerName = customerName.ifBlank { "Customer" },
            customerPhone = customerPhone.ifBlank { "N/A" },
            deliveryAddress = deliveryAddress,
            paymentMode = paymentMode,
            totalAmount = grandTotal,
            timestamp = timeStr,
            status = "CONFIRMED (COD)"
        )

        orderRepository.save(order)
        orderItemRepository.saveAll(itemsToSave)

        // Clear Cart
        cart.clear()

        return "redirect:/orders/success/$orderId"
    }

    @GetMapping("/orders/success/{orderId}")
    fun orderSuccess(@PathVariable("orderId") orderId: String, model: Model): String {
        val order = orderRepository.findByOrderId(orderId)
        val items = orderItemRepository.findByOrderId(orderId)

        model.addAttribute("order", order)
        model.addAttribute("items", items)
        return "order_success"
    }

    @GetMapping("/orders/history")
    fun orderHistory(principal: Principal?, model: Model): String {
        if (principal == null) return "redirect:/login"
        val orders = orderRepository.findByUserEmailOrderByTimestampDesc(principal.name)
        model.addAttribute("orders", orders)
        return "order_history"
    }
}

@Controller
@RequestMapping("/admin")
class AdminController(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository
) {
    @GetMapping("/dashboard")
    fun dashboard(
        @RequestParam(name = "editId", required = false) editId: Long?,
        model: Model
    ): String {
        val products = productRepository.findAllByOrderByNameAsc()
        val orders = orderRepository.findAllByOrderByTimestampDesc()

        val editingProduct = if (editId != null && editId > 0) {
            productRepository.findById(editId).orElse(ProductEntity())
        } else {
            ProductEntity()
        }

        model.addAttribute("products", products)
        model.addAttribute("orders", orders)
        model.addAttribute("product", editingProduct)
        model.addAttribute("isEditing", editId != null && editId > 0)
        return "admin_dashboard"
    }

    @PostMapping("/product/save")
    fun saveProduct(
        @RequestParam(name = "id", required = false) id: Long?,
        @RequestParam("name") name: String,
        @RequestParam("category") category: String,
        @RequestParam("price") price: Double,
        @RequestParam("unit") unit: String,
        @RequestParam(name = "stockQuantity", defaultValue = "100.0") stockQuantity: Double,
        @RequestParam(name = "description", defaultValue = "") description: String,
        @RequestParam(name = "imageUrl", defaultValue = "") imageUrlParam: String,
        @RequestParam(name = "imageFile", required = false) imageFile: MultipartFile?
    ): String {
        val product = if (id != null && id > 0) {
            productRepository.findById(id).orElse(ProductEntity())
        } else {
            ProductEntity()
        }

        product.name = name.trim()
        product.category = category.trim()
        product.price = price
        product.unit = unit.trim()
        product.stockQuantity = stockQuantity
        product.description = description.trim()

        // Handle image file upload if selected by admin
        if (imageFile != null && !imageFile.isEmpty) {
            try {
                val uploadDir = File("uploads")
                if (!uploadDir.exists()) uploadDir.mkdirs()

                val cleanOriginalName = imageFile.originalFilename?.replace("[^a-zA-Z0-9._-]".toRegex(), "_") ?: "photo.jpg"
                val uniqueFileName = "prod_${System.currentTimeMillis()}_$cleanOriginalName"
                val destinationFile = File(uploadDir, uniqueFileName)

                imageFile.transferTo(destinationFile)
                product.imageUrl = "/uploads/$uniqueFileName"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (imageUrlParam.isNotBlank()) {
            product.imageUrl = imageUrlParam.trim()
        }

        // Set fallback default image if still blank
        if (product.imageUrl.isBlank()) {
            product.imageUrl = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600"
        }

        productRepository.save(product)
        return "redirect:/admin/dashboard"
    }

    @GetMapping("/product/delete/{id}")
    fun deleteProduct(@PathVariable("id") id: Long): String {
        productRepository.deleteById(id)
        return "redirect:/admin/dashboard"
    }
}

@Controller
class InvoiceController(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository
) {
    @GetMapping("/invoice/{orderId}")
    fun downloadInvoice(@PathVariable("orderId") orderId: String, response: HttpServletResponse) {
        val order = orderRepository.findByOrderId(orderId) ?: return
        val items = orderItemRepository.findByOrderId(orderId)

        response.contentType = "application/pdf"
        response.setHeader("Content-Disposition", "attachment; filename=\"Invoice_${order.orderId}.pdf\"")

        val document = PDDocument()
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)

        val fontBold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        val fontRegular = PDType1Font(Standard14Fonts.FontName.HELVETICA)
        val fontOblique = PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE)

        val cs = PDPageContentStream(document, page)
        val width = PDRectangle.A4.width
        val height = PDRectangle.A4.height

        var y = height - 40f

        // Header Band
        cs.setNonStrokingColor(AwtColor(5, 150, 105)) // Organic Emerald Green
        cs.addRect(25f, y - 50f, width - 50f, 65f)
        cs.fill()

        cs.beginText()
        cs.setFont(fontBold, 18f)
        cs.setNonStrokingColor(AwtColor.WHITE)
        cs.newLineAtOffset(40f, y - 10f)
        cs.showText("ORGANIC & NATURAL FOODS")
        cs.endText()

        cs.beginText()
        cs.setFont(fontOblique, 9f)
        cs.newLineAtOffset(40f, y - 25f)
        cs.showText("100% Pure Grains, Pulses, Millets & Natural Spices")
        cs.endText()

        cs.beginText()
        cs.setFont(fontBold, 14f)
        cs.newLineAtOffset(width - 170f, y - 10f)
        cs.showText("TAX INVOICE")
        cs.endText()

        y -= 70f

        // Customer & Order Info Box
        cs.setNonStrokingColor(AwtColor(245, 247, 250))
        cs.addRect(30f, y - 60f, width - 60f, 60f)
        cs.fill()

        cs.setStrokingColor(AwtColor(226, 232, 240))
        cs.setLineWidth(1f)
        cs.addRect(30f, y - 60f, width - 60f, 60f)
        cs.stroke()

        cs.beginText()
        cs.setFont(fontBold, 10f)
        cs.setNonStrokingColor(AwtColor(15, 23, 42))
        cs.newLineAtOffset(40f, y - 16f)
        cs.showText("BILLED TO: ${order.customerName}")
        cs.newLineAtOffset(0f, -15f)
        cs.setFont(fontRegular, 9f)
        cs.showText("Phone: ${order.customerPhone}  |  Pay Mode: ${order.paymentMode}")
        cs.newLineAtOffset(0f, -15f)
        cs.showText("Address: ${order.deliveryAddress.take(45)}")
        cs.endText()

        cs.beginText()
        cs.setFont(fontBold, 10f)
        cs.newLineAtOffset(width - 210f, y - 16f)
        cs.showText("INVOICE NO: ${order.orderId}")
        cs.newLineAtOffset(0f, -15f)
        cs.setFont(fontRegular, 9f)
        cs.showText("Date: ${order.timestamp}")
        cs.endText()

        y -= 80f

        // Table Header
        cs.setNonStrokingColor(AwtColor(5, 150, 105))
        cs.addRect(30f, y - 20f, width - 60f, 22f)
        cs.fill()

        cs.beginText()
        cs.setFont(fontBold, 9.5f)
        cs.setNonStrokingColor(AwtColor.WHITE)
        cs.newLineAtOffset(40f, y - 14f)
        cs.showText("ITEM DESCRIPTION")
        cs.newLineAtOffset(250f, 0f)
        cs.showText("UNIT PRICE")
        cs.newLineAtOffset(80f, 0f)
        cs.showText("QTY")
        cs.newLineAtOffset(60f, 0f)
        cs.showText("TOTAL (RS)")
        cs.endText()

        y -= 20f

        // Items
        var sr = 1
        for (item in items) {
            val bg = if (sr % 2 == 0) AwtColor(248, 250, 252) else AwtColor.WHITE
            cs.setNonStrokingColor(bg)
            cs.addRect(30f, y - 18f, width - 60f, 18f)
            cs.fill()

            cs.beginText()
            cs.setFont(fontRegular, 8.5f)
            cs.setNonStrokingColor(AwtColor(30, 41, 59))
            cs.newLineAtOffset(40f, y - 13f)
            cs.showText(item.productName.take(38))
            cs.newLineAtOffset(250f, 0f)
            cs.showText("Rs.${"%.2f".format(item.unitPrice)}")
            cs.newLineAtOffset(80f, 0f)
            cs.showText("${item.quantity} ${item.unit}")
            cs.newLineAtOffset(60f, 0f)
            cs.setFont(fontBold, 8.5f)
            cs.showText("Rs.${"%.2f".format(item.subtotal)}")
            cs.endText()

            y -= 18f
            sr++
        }

        y -= 20f

        // Summary Box
        cs.setNonStrokingColor(AwtColor(241, 245, 249))
        cs.addRect(width - 240f, y - 45f, 210f, 45f)
        cs.fill()

        cs.setStrokingColor(AwtColor(5, 150, 105))
        cs.addRect(width - 240f, y - 45f, 210f, 45f)
        cs.stroke()

        cs.beginText()
        cs.setFont(fontBold, 11f)
        cs.setNonStrokingColor(AwtColor(5, 150, 105))
        cs.newLineAtOffset(width - 230f, y - 26f)
        cs.showText("GRAND TOTAL:")
        cs.newLineAtOffset(110f, 0f)
        cs.showText("Rs.${"%.2f".format(order.totalAmount)}")
        cs.endText()

        cs.close()

        val baos = ByteArrayOutputStream()
        document.save(baos)
        document.close()

        response.outputStream.write(baos.toByteArray())
        response.outputStream.flush()
    }
}
