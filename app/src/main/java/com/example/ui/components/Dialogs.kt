package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.StockChangeType
import com.example.model.StockLog
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import java.util.Calendar
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeSoft
import com.example.ui.theme.PurpleContainerLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDark
import com.example.ui.theme.PurplePrimaryDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.Formatters

val DEFAULT_CATEGORIES = listOf(
    "အချိုရည်နှင့် အသင့်သောက်",
    "စားသောက်ကုန်",
    "အလှကုန်နှင့် လူသုံးကုန်",
    "ဆေးဝါးနှင့် ကျန်းမာရေး",
    "အထည်အလိပ်",
    "လျှပ်စစ်နှင့် ဖုန်းပစ္စည်း",
    "အထွေထွေ (General)"
)

val DEFAULT_EXPENSE_CATEGORIES = listOf(
    "ပစ္စည်းဝယ်ယူစရိတ် (Stock)",
    "ဆိုင်ငှားခ (Shop Rent)",
    "မီးဖိုး / ရေဖိုး (Utilities)",
    "ဝန်ထမ်းလစာ (Salary)",
    "သယ်ယူပို့ဆောင်ခ (Transport)",
    "အထွေထွေ အသုံးစရိတ် (General)"
)

val DEFAULT_INCOME_CATEGORIES = listOf(
    "အရောင်းရငွေ (Sales)",
    "ဝန်ဆောင်မှုရငွေ (Service)",
    "အခြားဝင်ငွေ (Other Income)"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditProductDialog(
    initialProduct: Product? = null,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: DEFAULT_CATEGORIES[0]) }
    var stockQuantityStr by remember { mutableStateOf(initialProduct?.stockQuantity?.toString() ?: "10") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "ခု") }
    var costPriceStr by remember { mutableStateOf(if (initialProduct != null) Formatters.formatNumber(initialProduct.costPrice) else "1000") }
    var sellingPriceStr by remember { mutableStateOf(if (initialProduct != null) Formatters.formatNumber(initialProduct.sellingPrice) else "1300") }
    var lowThresholdStr by remember { mutableStateOf(initialProduct?.lowStockThreshold?.toString() ?: "5") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = PurplePrimary)
                Text(
                    text = if (initialProduct == null) "ပစ္စည်းအသစ် ထည့်သွင်းမည်" else "ပစ္စည်း အချက်အလက် ပြင်မည်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ပစ္စည်းအမည် (Product Name) *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = DarkOutline,
                        focusedLabelColor = PurplePrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Text("အမျိုးအစား (Category):", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DEFAULT_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainerLight,
                                selectedLabelColor = PurplePrimaryDeep,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = category == cat,
                                borderColor = if (category == cat) PurplePrimary else DarkOutline
                            )
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockQuantityStr,
                        onValueChange = { stockQuantityStr = it.filter { char -> char.isDigit() } },
                        label = { Text("စတင် Stock *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_stock_input")
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("ယူနစ် (ခု/ထုပ်/ဘူး)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("ဝယ်ဈေး (Cost MMK)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("ရောင်းဈေး (Sell MMK) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_sell_price_input")
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lowThresholdStr,
                        onValueChange = { lowThresholdStr = it.filter { char -> char.isDigit() } },
                        label = { Text("သတိပေး Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("ဘားကုဒ် / ကုဒ်") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val stock = stockQuantityStr.toIntOrNull() ?: 0
                        val cost = costPriceStr.replace(",", "").toDoubleOrNull() ?: 0.0
                        val sell = sellingPriceStr.replace(",", "").toDoubleOrNull() ?: 0.0
                        val low = lowThresholdStr.toIntOrNull() ?: 5
                        val product = (initialProduct ?: Product(name = name)).copy(
                            name = name.trim(),
                            barcode = barcode.trim(),
                            category = category,
                            stockQuantity = stock,
                            unit = if (unit.isBlank()) "ခု" else unit.trim(),
                            costPrice = cost,
                            sellingPrice = sell,
                            lowStockThreshold = low,
                            updatedAt = System.currentTimeMillis()
                        )
                        onConfirm(product)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleContainerLight,
                    contentColor = PurplePrimaryDeep
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(if (initialProduct == null) "ထည့်သွင်းမည်" else "သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Text("ပယ်ဖျက်မည်", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestockDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (addQuantity: Int, unitCost: Double, paymentMethod: PaymentMethod, recordAsExpense: Boolean, note: String) -> Unit
) {
    var addQuantityStr by remember { mutableStateOf("10") }
    var unitCostStr by remember { mutableStateOf(Formatters.formatNumber(product.costPrice)) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.KPAY) }
    var recordAsExpense by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    val addQuantity = addQuantityStr.toIntOrNull() ?: 0
    val unitCost = unitCostStr.replace(",", "").toDoubleOrNull() ?: 0.0
    val totalCost = addQuantity * unitCost

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PurplePrimary)
                Text("Stock ထပ်ဖြည့်မည်", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "လက်ရှိ Stock: ${product.stockQuantity} ${product.unit} ➔ ဖြည့်ပြီးပါက: ${product.stockQuantity + addQuantity} ${product.unit}",
                            fontSize = 13.sp,
                            color = PurplePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedTextField(
                    value = addQuantityStr,
                    onValueChange = { addQuantityStr = it.filter { char -> char.isDigit() } },
                    label = { Text("ထပ်ဖြည့်မည့် အရေအတွက် (${product.unit}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = DarkOutline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restock_qty_input")
                )

                OutlinedTextField(
                    value = unitCostStr,
                    onValueChange = { unitCostStr = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("၁ ${product.unit} လျှင် ဝယ်ရင်းဈေး (MMK)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = DarkOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Total Cost preview
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ExpenseColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("စုစုပေါင်း ကုန်ကျစရိတ်:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                        Text(Formatters.formatKyat(totalCost), fontWeight = FontWeight.Bold, color = ExpenseColor)
                    }
                }

                // Payment Method Selector
                Text("ငွေပေးချေသည့် နည်းလမ်း (Payment Method):", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PaymentMethod.KPAY, PaymentMethod.WAVE_PAY, PaymentMethod.CASH, PaymentMethod.CB_PAY, PaymentMethod.AYA_PAY).forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method.myanmarLabel, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainerLight,
                                selectedLabelColor = PurplePrimaryDeep,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                // Record as Expense Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ဆိုင်ထွက်ငွေစာရင်းထဲ ထည့်မည်", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("လချုပ်ရှင်းတမ်းတွင် ပစ္စည်းဝယ်ယူစရိတ်အဖြစ် တွက်ချက်မည်", fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(
                        checked = recordAsExpense,
                        onCheckedChange = { recordAsExpense = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PurplePrimaryDeep,
                            checkedTrackColor = PurplePrimary
                        )
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("မှတ်ချက် (ရွေးချယ်ရန်)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = DarkOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (addQuantity > 0) {
                        onConfirm(addQuantity, unitCost, paymentMethod, recordAsExpense, note)
                    }
                },
                enabled = addQuantity > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleContainerLight,
                    contentColor = PurplePrimaryDeep
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_restock_button")
            ) {
                Text("Stock ဖြည့်မည်", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Text("ပယ်ဖျက်မည်", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSaleDialog(
    products: List<Product>,
    preSelectedProduct: Product? = null,
    onDismiss: () -> Unit,
    onConfirm: (productId: Long, quantity: Int, unitPrice: Double, paymentMethod: PaymentMethod, note: String) -> Unit
) {
    var selectedProduct by remember { mutableStateOf(preSelectedProduct ?: products.firstOrNull()) }
    var quantityStr by remember { mutableStateOf("1") }
    var unitPriceStr by remember {
        mutableStateOf(
            if (selectedProduct != null) Formatters.formatNumber(selectedProduct!!.sellingPrice) else "0"
        )
    }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.KPAY) }
    var note by remember { mutableStateOf("") }

    val quantity = quantityStr.toIntOrNull() ?: 1
    val unitPrice = unitPriceStr.replace(",", "").toDoubleOrNull() ?: 0.0
    val totalAmount = quantity * unitPrice

    val currentStock = selectedProduct?.stockQuantity ?: 0
    val hasEnoughStock = currentStock >= quantity

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = IncomeColor)
                Text("အရောင်း စာရင်းသွင်းမည်", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (products.isEmpty()) {
                    Text("ပစ္စည်းစာရင်း မရှိသေးပါ။ ကျေးဇူးပြု၍ ပစ္စည်းအရင်ထည့်ပါ။", color = TextSecondary)
                } else {
                    Text("ရောင်းချမည့် ပစ္စည်း (Select Item):", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        products.forEach { prod ->
                            FilterChip(
                                selected = selectedProduct?.id == prod.id,
                                onClick = {
                                    selectedProduct = prod
                                    unitPriceStr = Formatters.formatNumber(prod.sellingPrice)
                                },
                                label = { Text("${prod.name} (${prod.stockQuantity} ${prod.unit})", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurpleContainerLight,
                                    selectedLabelColor = PurplePrimaryDeep,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    selectedProduct?.let { prod ->
                        Surface(
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "လက်ကျန် Stock: ${prod.stockQuantity} ${prod.unit} | ဝယ်ဈေး: ${Formatters.formatKyat(prod.costPrice)}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                if (!hasEnoughStock) {
                                    Text(
                                        "⚠️ လက်ကျန် မလုံလောက်ပါ!",
                                        fontSize = 12.sp,
                                        color = ExpenseColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it.filter { char -> char.isDigit() } },
                            label = { Text("အရေအတွက် *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = DarkOutline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sale_quantity_input")
                        )
                        OutlinedTextField(
                            value = unitPriceStr,
                            onValueChange = { unitPriceStr = it.filter { char -> char.isDigit() || char == '.' } },
                            label = { Text("ရောင်းဈေး (MMK) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = DarkOutline
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Total Preview
                    Surface(
                        color = IncomeContainer,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, IncomeColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ရောင်းရငွေ စုစုပေါင်း:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IncomeSoft)
                            Text(Formatters.formatKyat(totalAmount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IncomeColor)
                        }
                    }

                    Text("ငွေပေးချေမှု နည်းလမ်း (Payment Channel):", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(PaymentMethod.KPAY, PaymentMethod.WAVE_PAY, PaymentMethod.CASH, PaymentMethod.CB_PAY, PaymentMethod.AYA_PAY).forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method.myanmarLabel, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurpleContainerLight,
                                    selectedLabelColor = PurplePrimaryDeep,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("ဝယ်သူမှတ်ချက် / မှတ်ချက်") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedProduct != null && quantity > 0 && hasEnoughStock) {
                        onConfirm(selectedProduct!!.id, quantity, unitPrice, paymentMethod, note)
                    }
                },
                enabled = selectedProduct != null && quantity > 0 && hasEnoughStock,
                colors = ButtonDefaults.buttonColors(containerColor = IncomeColor, contentColor = Color(0xFF14532D)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_sale_button")
            ) {
                Text("ရောင်းချမှု မှတ်တမ်းတင်မည်", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Text("ပယ်ဖျက်မည်", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    initialType: TransactionType = TransactionType.INCOME,
    onDismiss: () -> Unit,
    onConfirm: (type: TransactionType, category: String, amount: Double, paymentMethod: PaymentMethod, note: String) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var amountStr by remember { mutableStateOf("") }
    var category by remember {
        mutableStateOf(
            if (initialType == TransactionType.INCOME) DEFAULT_INCOME_CATEGORIES[0] else DEFAULT_EXPENSE_CATEGORIES[0]
        )
    }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.KPAY) }
    var note by remember { mutableStateOf("") }

    val amount = amountStr.replace(",", "").toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = if (type == TransactionType.INCOME) IncomeColor else ExpenseColor)
                Text(
                    text = if (type == TransactionType.INCOME) "ဝင်ငွေ စာရင်းသွင်းမည်" else "ထွက်ငွေ စာရင်းသွင်းမည်",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            type = TransactionType.INCOME
                            category = DEFAULT_INCOME_CATEGORIES[0]
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == TransactionType.INCOME) IncomeColor else DarkSurfaceVariant,
                            contentColor = if (type == TransactionType.INCOME) Color(0xFF14532D) else TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("➕ ဝင်ငွေ (Income)", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            type = TransactionType.EXPENSE
                            category = DEFAULT_EXPENSE_CATEGORIES[0]
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == TransactionType.EXPENSE) ExpenseColor else DarkSurfaceVariant,
                            contentColor = if (type == TransactionType.EXPENSE) Color(0xFF601410) else TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("➖ ထွက်ငွေ (Expense)", fontWeight = FontWeight.SemiBold)
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("ငွေပမာဏ (Amount MMK) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = DarkOutline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input")
                )

                Text("အမျိုးအစား (Category):", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                val categories = if (type == TransactionType.INCOME) DEFAULT_INCOME_CATEGORIES else DEFAULT_EXPENSE_CATEGORIES
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainerLight,
                                selectedLabelColor = PurplePrimaryDeep,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Text("ငွေပေးချေမှု (KPay / Wave / Cash):", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PaymentMethod.KPAY, PaymentMethod.WAVE_PAY, PaymentMethod.CASH, PaymentMethod.CB_PAY, PaymentMethod.AYA_PAY).forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method.myanmarLabel, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurpleContainerLight,
                                selectedLabelColor = PurplePrimaryDeep,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("မှတ်ချက် (Note)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = DarkOutline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (amount > 0) {
                        onConfirm(type, category, amount, paymentMethod, note)
                    }
                },
                enabled = amount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == TransactionType.INCOME) IncomeColor else ExpenseColor,
                    contentColor = if (type == TransactionType.INCOME) Color(0xFF14532D) else Color(0xFF601410)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_add_tx_button")
            ) {
                Text("မှတ်တမ်းတင်မည်", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Text("ပယ်ဖျက်မည်", color = TextSecondary)
            }
        }
    )
}

@Composable
fun ProductHistoryDialog(
    product: Product,
    logs: List<StockLog>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Stock ရာဇဝင်မှတ်တမ်း", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text(product.name, fontSize = 13.sp, color = PurplePrimary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "ပိတ်မည်", tint = TextSecondary)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DarkOutline.copy(alpha = 0.4f))

                if (logs.isEmpty()) {
                    Text(
                        "ဤပစ္စည်းအတွက် အဝင်/အထွက် မှတ်တမ်း မရှိသေးပါ။",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(logs) { log ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val isRestock = log.changeType == StockChangeType.RESTOCK
                                            Icon(
                                                imageVector = if (isRestock) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (isRestock) IncomeColor else ExpenseColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = log.changeType.myanmarLabel,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            log.paymentMethod?.let {
                                                PaymentBadge(method = it, compact = true)
                                            }
                                        }
                                        Text(
                                            text = "${log.previousStock} ➔ ${log.newStock} ${product.unit} (${if (log.quantityChanged > 0) "+${log.quantityChanged}" else "${log.quantityChanged}"})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PurplePrimary
                                        )
                                        if (log.note.isNotBlank()) {
                                            Text(log.note, fontSize = 11.sp, color = TextMuted)
                                        }
                                    }
                                    Text(
                                        text = Formatters.formatDateTime(log.dateMillis),
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(
    transaction: TransactionRecord,
    onDismiss: () -> Unit,
    onDelete: (TransactionRecord) -> Unit
) {
    val isIncome = transaction.type == TransactionType.INCOME

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isIncome) IncomeColor else ExpenseColor
                    )
                    Text(
                        text = if (isIncome) "ဝင်ငွေ အသေးစိတ် စာရင်း" else "ထွက်ငွေ အသေးစိတ် စာရင်း",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "ပိတ်မည်", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large Amount Card
                Surface(
                    color = if (isIncome) IncomeContainer else ExpenseContainer,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, (if (isIncome) IncomeColor else ExpenseColor).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isIncome) "ဝင်ငွေ ပမာဏ" else "ထွက်ငွေ ပမာဏ",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (isIncome) "+ " else "- "}${Formatters.formatKyat(transaction.amount)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) IncomeColor else ExpenseColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        PaymentBadge(method = transaction.paymentMethod)
                    }
                }

                // Metadata list
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailItemRow("စာရင်းအမျိုးအစား", transaction.category)
                        DetailItemRow("ရက်စွဲနှင့် အချိန်", Formatters.formatDateTime(transaction.dateMillis))
                        DetailItemRow("ငွေချေမှုပုံစံ", transaction.paymentMethod.myanmarLabel)

                        if (!transaction.productName.isNullOrBlank()) {
                            DetailItemRow("သက်ဆိုင်ရာ ပစ္စည်း", transaction.productName)
                        }
                        if (transaction.quantity != null && transaction.quantity > 0) {
                            DetailItemRow("အရေအတွက်", "${transaction.quantity} ခု")
                        }
                        if (transaction.unitPrice != null && transaction.unitPrice > 0) {
                            DetailItemRow("တစ်ခုဈေး", Formatters.formatKyat(transaction.unitPrice))
                        }
                        if (transaction.note.isNotBlank()) {
                            DetailItemRow("မှတ်ချက်", transaction.note)
                        }
                        DetailItemRow("မှတ်တမ်း ID", "#${transaction.id}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleContainerLight,
                    contentColor = PurplePrimaryDeep
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ပိတ်မည်", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    onDelete(transaction)
                    onDismiss()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseColor),
                border = BorderStroke(1.dp, ExpenseColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ဖျက်မည် 🗑️")
            }
        }
    )
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
fun CustomDateRangePickerDialog(
    initialStartMillis: Long? = null,
    initialEndMillis: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit
) {
    val now = Calendar.getInstance()
    var startCal by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                if (initialStartMillis != null) {
                    timeInMillis = initialStartMillis
                } else {
                    add(Calendar.DAY_OF_MONTH, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
        )
    }

    var endCal by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                if (initialEndMillis != null) {
                    timeInMillis = initialEndMillis
                } else {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📅 ရက်စွဲအပိုင်းအခြား ရွေးချယ်ရန်", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "စတင်မည့်ရက်နှင့် ပြီးဆုံးမည့်ရက်ကို ရွေးချယ်ပေးပါ:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // Start Date Selector Box
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("စတင်မည့်ရက် (From Date):", fontSize = 11.sp, color = PurplePrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatDate(startCal.timeInMillis),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = startCal.timeInMillis
                                        add(Calendar.DAY_OF_MONTH, -1)
                                    }
                                    startCal = newCal
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                            ) {
                                Text("-1 ရက်", fontSize = 11.sp, color = TextSecondary)
                            }
                            OutlinedButton(
                                onClick = {
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = startCal.timeInMillis
                                        add(Calendar.DAY_OF_MONTH, 1)
                                    }
                                    startCal = newCal
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                            ) {
                                Text("+1 ရက်", fontSize = 11.sp, color = TextSecondary)
                            }
                            OutlinedButton(
                                onClick = {
                                    val newCal = Calendar.getInstance().apply {
                                        add(Calendar.DAY_OF_MONTH, -7)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                    }
                                    startCal = newCal
                                },
                                modifier = Modifier.weight(1.3f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                            ) {
                                Text("၇ ရက်အလို", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                // End Date Selector Box
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("ပြီးဆုံးမည့်ရက် (To Date):", fontSize = 11.sp, color = PurplePrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatDate(endCal.timeInMillis),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = endCal.timeInMillis
                                        add(Calendar.DAY_OF_MONTH, -1)
                                    }
                                    endCal = newCal
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                            ) {
                                Text("-1 ရက်", fontSize = 11.sp, color = TextSecondary)
                            }
                            OutlinedButton(
                                onClick = {
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = endCal.timeInMillis
                                        add(Calendar.DAY_OF_MONTH, 1)
                                    }
                                    endCal = newCal
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                            ) {
                                Text("+1 ရက်", fontSize = 11.sp, color = TextSecondary)
                            }
                            OutlinedButton(
                                onClick = {
                                    val newCal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 23)
                                        set(Calendar.MINUTE, 59)
                                        set(Calendar.SECOND, 59)
                                    }
                                    endCal = newCal
                                },
                                modifier = Modifier.weight(1.3f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                            ) {
                                Text("ယနေ့အထိ", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = Calendar.getInstance().apply {
                        timeInMillis = startCal.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val e = Calendar.getInstance().apply {
                        timeInMillis = endCal.timeInMillis
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    onConfirm(s, e)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleContainerLight,
                    contentColor = PurplePrimaryDeep
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ရွေးချယ်မည်", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Text("ပယ်ဖျက်မည်", color = TextSecondary)
            }
        }
    )
}

