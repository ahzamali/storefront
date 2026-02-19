package com.storefront.app.ui.pos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storefront.app.viewmodel.CartViewModel
import java.math.BigDecimal

@Composable
fun CartSection(viewModel: CartViewModel, onCheckoutClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Current Cart", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(viewModel.cartItems) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(item.sku, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.removeOne(item) }, modifier = Modifier.size(24.dp)) {
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { }, modifier = Modifier.size(24.dp)) { }
                    }
                    
                    Text(
                        "₹${item.price.multiply(BigDecimal(item.quantity))}", 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End
                    )
                }
                Divider()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total: ₹${viewModel.totalAmount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(
                onClick = onCheckoutClick,
                enabled = viewModel.cartItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Checkout")
            }
        }
    }
}
