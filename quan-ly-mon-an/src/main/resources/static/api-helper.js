/**
 * API Helper - Multi-Tenancy Support
 * Sử dụng file này để gọi API với tự động truyền User ID
 */

class APIHelper {
  constructor() {
    this.baseUrl = '/api';
    this.userId = localStorage.getItem('userId') || null;
  }

  /**
   * Đặt User ID
   */
  setUserId(userId) {
    this.userId = userId;
    localStorage.setItem('userId', userId);
  }

  /**
   * Xóa User ID (logout)
   */
  clearUserId() {
    this.userId = null;
    localStorage.removeItem('userId');
  }

  /**
   * Tạo headers với User ID
   */
  getHeaders(additionalHeaders = {}) {
    const headers = {
      'Content-Type': 'application/json',
      ...additionalHeaders
    };
    
    if (this.userId) {
      headers['X-User-Id'] = this.userId;
    }
    
    return headers;
  }

  /**
   * Generic fetch method
   */
  async request(endpoint, options = {}) {
    const url = `${this.baseUrl}${endpoint}`;
    const config = {
      ...options,
      headers: this.getHeaders(options.headers || {})
    };

    try {
      console.log(`📡 API Call: ${config.method || 'GET'} ${url}`);
      const response = await fetch(url, config);
      
      if (!response.ok) {
        let errorMsg = `HTTP ${response.status}: ${response.statusText}`;
        try {
          const errorData = await response.json();
          errorMsg = errorData.message || errorData.error || errorMsg;
        } catch (e) {
          const text = await response.text();
          if (text) errorMsg = text;
        }
        console.error(`❌ API Error (${response.status}):`, errorMsg);
        throw new Error(errorMsg);
      }
      
      const data = await response.json();
      console.log(`✅ API Success:`, data);
      return data;
    } catch (error) {
      console.error(`❌ API Request Error (${endpoint}):`, error.message);
      throw error;
    }
  }

  // ============= AUTH API =============
  
  /**
   * Đăng nhập
   */
  async login(username, password) {
    const data = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
    
    if (data.success) {
      this.setUserId(data.data.id);
    }
    
    return data;
  }

  /**
   * Đăng ký
   */
  async register(username, password, hoTen) {
    return await this.request('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, password, hoTen })
    });
  }

  /**
   * Đăng nhập qua Google
   */
  async loginWithGoogle(idToken) {
    const data = await this.request('/auth/google-login', {
      method: 'POST',
      body: JSON.stringify({ idToken })
    });
    
    if (data.success) {
      this.setUserId(data.data.id);
    }
    
    return data;
  }

  // ============= MON AN API =============
  
  /**
   * Lấy danh sách món ăn
   */
  async getMonAn() {
    return await this.request('/mon-an');
  }

  /**
   * Tìm kiếm món ăn
   */
  async searchMonAn(keyword) {
    return await this.request(`/mon-an/search?kw=${encodeURIComponent(keyword)}`);
  }

  /**
   * Thêm/cập nhật món ăn
   */
  async saveMonAn(monAn) {
    // Nếu có ID, sử dụng PUT (cập nhật); không có ID, sử dụng POST (thêm mới)
    if (monAn.id) {
      return await this.request(`/mon-an/${monAn.id}`, {
        method: 'PUT',
        body: JSON.stringify(monAn)
      });
    } else {
      return await this.request('/mon-an', {
        method: 'POST',
        body: JSON.stringify(monAn)
      });
    }
  }

/**
   * Xóa món ăn
   */
  async deleteMonAn(id) {
    return await this.request(`/mon-an/${id}`, {
      method: 'DELETE'
    });
  }

  /**
   * Upload ảnh
   */
  async uploadImage(file) {
    const formData = new FormData();
    formData.append('file', file);

    return await fetch(`${this.baseUrl}/mon-an/upload`, {
      method: 'POST',
      body: formData,
      headers: {
        'X-User-Id': this.userId || ''
      }
    }).then(res => res.text());
  }

  // ============= BAN API =============
  
  /**
   * Lấy danh sách bàn
   */
  async getBan() {
    return await this.request('/ban');
  }

  /**
   * Tạo bàn
   */
  async taoBan(soBan) {
    return await this.request('/ban/tao-ban', {
      method: 'POST',
      body: JSON.stringify({ soBan, trangThai: 'TRONG' })
    });
  }

  /**
   * Reset bàn — FIX: đổi sang đúng endpoint OrderController
   */
  async resetBan(soBan) {
    return await this.request(`/orders/reset-ban/${encodeURIComponent(soBan)}`, {
      method: 'POST'
    });
  }

  /**
   * Xóa bàn
   */
  async xoaBan(soBan) {
    return await this.request(`/ban/${soBan}`, {
      method: 'DELETE'
    });
  }

  // ============= DANH MUC API =============
  
  /**
   * Lấy danh sách danh mục
   */
  async getDanhMuc() {
    return await this.request('/danh-muc');
  }

  // ============= ORDER API =============
  
  /**
   * Lấy danh sách bàn (cho gọi món)
   */
  async getOrderBans() {
    return await this.request('/orders/danh-sach-ban');
  }

  /**
   * Gửi đơn
   */
  async guiDon(soBan, tongTien, items) {
    return await this.request('/orders/gui-don', {
      method: 'POST',
      body: JSON.stringify({ soBan, tongTien, items })
    });
  }

  /**
   * Lấy chi tiết bàn
   */
  async getChiTietBan(soBan) {
    return await this.request(`/orders/chi-tiet-ban/${encodeURIComponent(soBan)}`);
  }

  /**
   * Thanh toán — FIX: chỉ gửi tongTien, không gửi items (backend không cần)
   */
  async thanhToan(soBan, tongTien) {
    return await this.request(`/orders/thanh-toan/${encodeURIComponent(soBan)}`, {
      method: 'POST',
      body: JSON.stringify({ tongTien })
    });
  }

  /**
   * Khởi tạo sơ đồ bàn
   */
  async khoiTaoSoDo(soLuong) {
    return await this.request('/orders/khoi-tao-so-do', {
      method: 'POST',
      body: JSON.stringify({ soLuong })
    });
  }

  /**
   * Cập nhật bàn
   */
  async capNhatBan(soBan, items) {
    return await this.request(`/orders/cap-nhat-ban/${encodeURIComponent(soBan)}`, {
      method: 'POST',
      body: JSON.stringify(items)
    });
  }

  /**
   * Xóa bàn (order)
   */
  async xoaBanOrder(soBan) {
    return await this.request(`/orders/xoa-ban/${encodeURIComponent(soBan)}`, {
      method: 'DELETE'
    });
  }

  // ============= THONG KE API =============
  
  /**
   * Thống kê hôm nay — FIX: đổi sang đúng endpoint OrderController
   */
  async thongKeHomNay() {
    return await this.request('/orders/thong-ke-hom-nay');
  }

  /**
   * Thống kê theo ngày
   */
  async thongKeTheoNgay(from, to) {
    return await this.request(`/thong-ke/theo-ngay?from=${from}&to=${to}`);
  }

  /**
   * Thống kê theo tháng
   */
  async thongKeTheoThang(year) {
    return await this.request(`/thong-ke/theo-thang?year=${year}`);
  }

  /**
   * Thống kê theo quý
   */
  async thongKeTheoQuy(year) {
    return await this.request(`/thong-ke/theo-quy?year=${year}`);
  }

  /**
   * Top món
   */
  async topMon(from, to, year) {
    let url = '/thong-ke/top-mon';
    if (from && to) {
      url += `?from=${from}&to=${to}`;
    } else if (year) {
      url += `?year=${year}`;
    }
    return await this.request(url);
  }

  /**
   * Lấy danh sách đơn hàng theo ngày
   */
  async donHangTheoNgay(ngay) {
    return await this.request(`/thong-ke/don-hang-theo-ngay?ngay=${ngay}`);
  }

  /**
   * Lấy danh sách đơn hàng theo tháng
   */
  async donHangTheoThang(thang, nam) {
    return await this.request(`/thong-ke/don-hang-theo-thang?thang=${thang}&nam=${nam}`);
  }

  /**
   * Lấy chi tiết một đơn hàng
   */
  async chiTietDonHang(donHangId) {
    return await this.request(`/thong-ke/chi-tiet-don-hang/${donHangId}`);
  }

  /**
   * Generic GET method (để dùng trong các function khác)
   */
  async get(endpoint) {
    return await this.request(endpoint);
  }
}

// Khởi tạo global instance
const api = new APIHelper();