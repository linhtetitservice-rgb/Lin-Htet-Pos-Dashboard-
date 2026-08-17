package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.StockLog
import com.example.ui.components.AddEditProductDialog
import com.example.ui.components.DEFAULT_CATEGORIES
import com.example.ui.components.ProductHistoryDialog
import com.example.ui.components.QuickSaleDialog
import com.example.ui.components.RestockDialog
import com.example.ui.components.StockBadge
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.IncomeSoft
import com.example.ui.theme.PurpleContainerLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningColor
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryScreen(
    viewModel: ShopViewModel
) {
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val allProductsList by viewModel.products.collectAsStateWithLifecycle()
    val allLogs by viewModel.stockLogs.collectAsStateWithLifecycle()

    val searchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val showOnlyLowStock by viewModel.showOnlyLowStock.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var restockProduct by remember { mutableStateOf<Product?>(null) }
    var saleProduct by remember { mutableStateOf<Product?>(null) }
    var historyProduct by remember { mutableStateOf<Product?>(null) }
    var deleteCandidateProduct by remember { mutableStateOf<Product?>(null) }

    val totalCostValue = allProductsList.sumOf { it.totalCostValue }
    val totalRetailValue = allProductsList.sumOf { it.totalRetailValue }
    val lowStockCount = allProductsList.count { it.isLowStock }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PurpleContainerLight,
                contentColor = PurplePrimaryDeep,
                modifier = Modifier.testTag("fab_add_product")
            ) {
                Icon(Icons.Default.Add, contentDescription = "ပစ္စည်းအသစ်ထည့်မည်")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
                .testTag("inventory_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Inventory Value Header Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📦 ဆိုင်ရှိ Stock ပစ္စည်းများ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkOutline.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${allProductsList.size} မျိုး",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleContainerLight,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ဝယ်ရင်းတန်ဖိုး (Cost)", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        Formatters.formatKyat(totalCostValue),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, IncomeColor.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ရောင်းဈေးတန်ဖိုး (Retail)", fontSize = 11.sp, color = IncomeSoft)
                                    Text(
                                        Formatters.formatKyat(totalRetailValue),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurface,
                        border = BorderStroke(
                            1.dp,
                            if (searchQuery.isNotBlank()) PurplePrimary else DarkOutline.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.productSearchQuery.value = it },
                            placeholder = {
                                Text(
                                    "ပစ္စည်းအမည် (သို့) SKU / Barcode ဖြင့် ရှာရန်...",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (searchQuery.isNotBlank()) PurplePrimary else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = { viewModel.productSearchQuery.value = "" },
                                        modifier = Modifier.testTag("clear_search_btn")
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "ရှင်းမည်",
                                            tint = PurplePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("inventory_search_bar")
                        )
                    }

                    // Active Search Query Banner (if searching)
                    if (searchQuery.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "‘$searchQuery’ အတွက် ရှာတွေ့မှု: ${products.size} မျိုး",
                                fontSize = 12.sp,
                                color = PurplePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "အားလုံးပြန်ပြမည် (Clear)",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.productSearchQuery.value = "" }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Category & Low Stock Filter Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == "All",
                                onClick = { viewModel.selectedCategory.value = "All" },
                                label = { Text("အားလုံး", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurpleContainerLight,
                                    selectedLabelColor = PurplePrimaryDeep,
                                    containerColor = DarkSurface,
                                    labelColor = TextSecondary
                                )
                            )
                        }

                        item {
                            FilterChip(
                                selected = showOnlyLowStock,
                                onClick = { viewModel.showOnlyLowStock.value = !showOnlyLowStock },
                                label = { Text("⚠️ ကုန်ခါနီးသာ ($lowStockCount)", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WarningColor,
                                    selectedLabelColor = Color.Black,
                                    containerColor = DarkSurface,
                                    labelColor = WarningColor
                                )
                            )
                        }

                        items(DEFAULT_CATEGORIES) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { viewModel.selectedCategory.value = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurpleContainerLight,
                                    selectedLabelColor = PurplePrimaryDeep,
                                    containerColor = DarkSurface,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Empty state
            if (products.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotBlank()) Icons.Default.Search else Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = PurplePrimary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "‘$searchQuery’ ဖြင့် ကိုက်ညီသော ပစ္စည်း ရှာမတွေ့ပါ"
                                else
                                    "ပစ္စည်း ရှာမတွေ့ပါ သို့မဟုတ် မရှိသေးပါ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (searchQuery.isNotBlank() || selectedCategory != "All" || showOnlyLowStock) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.productSearchQuery.value = ""
                                        viewModel.selectedCategory.value = "All"
                                        viewModel.showOnlyLowStock.value = false
                                    },
                                    border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.5f))
                                ) {
                                    Text("ရှာဖွေမှု/Filter များ ရှင်းထုတ်မည်", color = PurplePrimary, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PurpleContainerLight,
                                        contentColor = PurplePrimaryDeep
                                    )
                                ) {
                                    Text("ပစ္စည်းအသစ် ထည့်သွင်းမည် ➕", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Products List
            items(products, key = { it.id }) { product ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_card_${product.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = product.category,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    if (product.barcode.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = PurplePrimary.copy(alpha = 0.12f),
                                            border = BorderStroke(0.5.dp, PurplePrimary.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "SKU: ${product.barcode}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = PurplePrimary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            StockBadge(
                                stockQuantity = product.stockQuantity,
                                lowThreshold = product.lowStockThreshold,
                                unit = product.unit
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pricing Grid
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("ဝယ်ရင်း: ${Formatters.formatKyat(product.costPrice)}", fontSize = 12.sp, color = TextSecondary)
                                    Text(
                                        "ရောင်းဈေး: ${Formatters.formatKyat(product.sellingPrice)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeColor
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "အမြတ်: +${Formatters.formatKyat(product.profitMarginPerUnit)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PurplePrimary
                                    )
                                    Text(
                                        "(${String.format("%.1f", product.profitPercentage)}% margin)",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons on product
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { restockProduct = product },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = PurplePrimary),
                                border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_restock_${product.id}")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stock ဖြည့်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { saleProduct = product },
                                colors = ButtonDefaults.buttonColors(containerColor = IncomeColor, contentColor = Color(0xFF14532D)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_sale_${product.id}")
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ရောင်းမည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { historyProduct = product },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = "ရာဇဝင်", tint = PurplePrimary)
                            }

                            IconButton(
                                onClick = { editingProduct = product },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "ပြင်ဆင်မည်", tint = TextSecondary)
                            }

                            IconButton(
                                onClick = { deleteCandidateProduct = product },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "ဖျက်မည်", tint = ExpenseColor)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AddEditProductDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newProd ->
                viewModel.addProduct(newProd)
                showAddDialog = false
            }
        )
    }

    if (editingProduct != null) {
        AddEditProductDialog(
            initialProduct = editingProduct,
            onDismiss = { editingProduct = null },
            onConfirm = { updated ->
                viewModel.updateProduct(updated)
                editingProduct = null
            }
        )
    }

    if (restockProduct != null) {
        RestockDialog(
            product = restockProduct!!,
            onDismiss = { restockProduct = null },
            onConfirm = { addQty, cost, method, recordExpense, note ->
                viewModel.restockProduct(
                    productId = restockProduct!!.id,
                    addQuantity = addQty,
                    costPerUnit = cost,
                    paymentMethod = method,
                    recordAsExpense = recordExpense,
                    note = note
                )
                restockProduct = null
            }
        )
    }

    if (saleProduct != null) {
        QuickSaleDialog(
            products = allProductsList,
            preSelectedProduct = saleProduct,
            onDismiss = { saleProduct = null },
            onConfirm = { prodId, qty, price, method, note ->
                viewModel.quickSale(prodId, qty, price, method, note)
                saleProduct = null
            }
        )
    }

    if (historyProduct != null) {
        val prodLogs = allLogs.filter { it.productId == historyProduct!!.id }
        ProductHistoryDialog(
            product = historyProduct!!,
            logs = prodLogs,
            onDismiss = { historyProduct = null }
        )
    }

    if (deleteCandidateProduct != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidateProduct = null },
            containerColor = DarkSurface,
            title = { Text("ပစ္စည်း ဖျက်ရန် သေချာပါသလား?", color = TextPrimary) },
            text = { Text("${deleteCandidateProduct!!.name} ကို စာရင်းထဲမှ အပြီးတိုင် ဖျက်မည်။", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(deleteCandidateProduct!!)
                        deleteCandidateProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseColor, contentColor = Color(0xFF601410))
                ) {
                    Text("ဖျက်မည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deleteCandidateProduct = null },
                    border = BorderStroke(1.dp, DarkOutline)
                ) {
                    Text("မဖျက်တော့ပါ", color = TextSecondary)
                }
            }
        )
    }
}
