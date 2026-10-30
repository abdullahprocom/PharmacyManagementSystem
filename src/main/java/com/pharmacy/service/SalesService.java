package com.pharmacy.service;

import com.pharmacy.dao.SalesDAO;
import com.pharmacy.model.CartItem;
import com.pharmacy.model.Sale;
import com.pharmacy.model.SaleItem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class SalesService {

    private final SalesDAO salesDAO = new SalesDAO();

    public void completeSale(Sale sale, List<CartItem> cart) throws Exception {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("لا يمكن إتمام البيع بسلة مشتريات فارغة.");
        }
        if (sale.getPaymentMethod() == null || (!sale.getPaymentMethod().equals("CASH") && !sale.getPaymentMethod().equals("CARD"))) {
            throw new IllegalArgumentException("يجب اختيار طريقة دفع صحيحة (نقداً CASH أو بطاقة CARD).");
        }

        if (sale.getSaleDate() == null || sale.getSaleDate().isBlank()) {
            sale.setSaleDate(LocalDate.now().toString());
        }

        long grossPiasters = 0;
        for (CartItem item : cart) {
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("كمية الدواء يجب أن تكون أكبر من الصفر: " + item.getMedicineName());
            }
            if (item.getQuantity() > item.getMedicine().getSellableStock()) {
                throw new IllegalArgumentException("الكمية المطلوبة (" + item.getQuantity() 
                    + ") تتجاوز الرصيد الصالح للبيع (" + item.getMedicine().getSellableStock() 
                    + ") لدواء: " + item.getMedicineName());
            }
            grossPiasters = Math.addExact(grossPiasters, item.getSubtotalPiasters());
        }

        // If totalAmountPiasters was not explicitly set (or 0), use gross subtotal
        if (sale.getTotalAmountPiasters() <= 0) {
            sale.setTotalAmountPiasters(grossPiasters);
        } else if (sale.getTotalAmountPiasters() > grossPiasters) {
            // Cannot charge more than total
            sale.setTotalAmountPiasters(grossPiasters);
        }

        salesDAO.processSale(sale, cart);
    }

    public List<Sale> getHistory(String start, String end) throws SQLException {
        return salesDAO.getSalesHistory(start, end);
    }

    public List<SaleItem> getDetails(int saleId) throws SQLException {
        return salesDAO.getSaleDetails(saleId);
    }
}
