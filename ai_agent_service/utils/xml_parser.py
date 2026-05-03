import xml.etree.ElementTree as ET
import re
from datetime import datetime

def parseVietnameseEInvoiceXML(file_content):
    """
    Bóc tách dữ liệu hóa đơn điện tử Việt Nam theo Nghị định 123/2020/NĐ-CP
    Xử lý thông minh việc xóa Namespace để tìm thẻ dễ dàng hơn.
    """
    try:
        # 1. Chuyển đổi sang string và Xóa Namespace
        if isinstance(file_content, bytes):
            file_content = file_content.decode('utf-8')
            
        # Xóa các khai báo namespace để find() hoạt động bình thường mà không cần tiền tố
        xml_string = re.sub(r'\sxmlns="[^"]+"', '', file_content, count=1)
        root = ET.fromstring(xml_string)
        
        # Hàm hỗ trợ lấy text an toàn
        def get_node_text(path):
            node = root.find(f".//{path}")
            return node.text if node is not None else None

        # 2. Bóc tách danh sách sản phẩm (Line Items)
        # Theo file sếp gửi: <HHDVu>, tên hàng là <THHDVu>
        items = []
        item_nodes = root.findall(".//HHDVu")
        for node in item_nodes:
            # Ưu tiên lấy THHDVu (Tên hàng hóa dịch vụ)
            item_name_node = node.find("THHDVu")
            if item_name_node is None:
                item_name_node = node.find("Ten") # Fallback cho các bản cũ
            
            item_name = item_name_node.text if item_name_node is not None else ""
            item_qty = float(node.find("SLuong").text or 0) if node.find("SLuong") is not None else 0
            item_price = float(node.find("DGia").text or 0) if node.find("DGia") is not None else 0
            
            if item_name:
                items.append({
                    "productName": item_name,
                    "quantity": int(item_qty),
                    "unitPrice": item_price
                })

        # 3. Trích xuất dữ liệu tổng quan
        n_ban = root.find(".//NBan")
        n_mua = root.find(".//NMua")
        
        data = {
            "invoiceSymbol": get_node_text("KHHDon"),
            "invoiceNumber": get_node_text("SHDon"),
            "invoiceDate": get_node_text("NLap"),
            "sellerName": n_ban.find("Ten").text if (n_ban is not None and n_ban.find("Ten") is not None) else None,
            "sellerTaxCode": n_ban.find("MST").text if (n_ban is not None and n_ban.find("MST") is not None) else None,
            "buyerName": n_mua.find("Ten").text if (n_mua is not None and n_mua.find("Ten") is not None) else None,
            "buyerTaxCode": n_mua.find("MST").text if (n_mua is not None and n_mua.find("MST") is not None) else None,
            "totalAmount": float(get_node_text("TgTCThue") or get_node_text("TgTien") or 0),
            "taxAmount": float(get_node_text("TgTThue") or 0),
            "finalAmount": float(get_node_text("TgTTTBSo") or 0),
            "items": items
        }
        
        # Bóc tách thuế suất (Tax Rate)
        tax_rate_node = root.find(".//TSuat")
        if tax_rate_node is not None:
            rate_text = tax_rate_node.text or ""
            if "10" in rate_text: data["taxRate"] = 0.1
            elif "8" in rate_text: data["taxRate"] = 0.08
            elif "5" in rate_text: data["taxRate"] = 0.05
            else: data["taxRate"] = 0.0
        else:
            data["taxRate"] = 0.1

        # 4. Chuẩn hóa ngày tháng (YYYY-MM-DD)
        if data["invoiceDate"]:
            # Thử các format phổ biến
            for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%Y%m%d"):
                try:
                    data["invoiceDate"] = datetime.strptime(data["invoiceDate"][:10], fmt).strftime("%Y-%m-%d")
                    break
                except:
                    continue
                    
        return data
        
    except Exception as e:
        print(f"Error parsing XML: {e}")
        raise Exception(f"Lỗi khi bóc tách hóa đơn: {str(e)}")
