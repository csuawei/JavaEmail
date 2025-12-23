<template>
  <div class="contact-wrapper">
    <!-- 操作栏 -->
    <div class="contact-operate-bar">
      <el-button 
        type="primary" 
        icon="el-icon-plus" 
        size="mini" 
        @click="openDialog(false)"
      >
        新增联系人
      </el-button>
      <el-button 
        type="text" 
        icon="el-icon-delete" 
        size="mini" 
        @click="batchDeleteContacts"
        :disabled="selectedContactIds.length === 0"
      >
        批量删除
      </el-button>
      <el-input
        placeholder="搜索联系人..."
        prefix-icon="el-icon-search"
        v-model="searchKeyword"
        size="mini"
        style="width: 200px; margin-left: 10px;"
        @input="handleSearch"
      ></el-input>
    </div>

    <!-- 联系人列表 -->
    <el-card shadow="hover" class="contact-list-card">
      <!-- 加载状态 -->
      <div class="loading-container" v-if="loading">
        <el-icon class="loading-icon"><i class="el-icon-loading"></i></el-icon>
        <span class="loading-text">正在加载联系人...</span>
      </div>

      <!-- 无数据状态 -->
      <div class="empty-container" v-else-if="contactList.length === 0">
        <el-icon class="empty-icon"><i class="el-icon-user"></i></el-icon>
        <span class="empty-text">暂无联系人</span>
        <el-button 
          type="primary" 
          size="mini" 
          @click="openDialog(false)"
          class="add-btn"
        >
          添加第一个联系人
        </el-button>
      </div>

      <!-- 联系人表格 -->
      <el-table
        :data="filteredContacts"
        @selection-change="handleSelectionChange"
        border
        fit
        style="width: 100%;"
      >
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column prop="contactName" label="姓名" width="120"></el-table-column>
        <el-table-column prop="contactEmail" label="邮箱" width="200"></el-table-column>
        <el-table-column prop="contactGroup" label="分组" width="120">
          <template slot-scope="scope">
            <el-tag size="mini">{{ scope.row.contactGroup || '未分组' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="150"></el-table-column>
        <el-table-column prop="remark" label="备注"></el-table-column>
        <el-table-column label="操作" width="180">
          <template slot-scope="scope">

            <el-button 
              type="text" 
              size="mini" 
              @click="writeLetter(scope.row.contactEmail)"
            >
              写信
            </el-button>
            <el-button 
              type="text" 
              size="mini" 
              @click="openDialog(true, scope.row)"
            >
              编辑
            </el-button>
            <el-button 
              type="text" 
              size="mini" 
              text-color="#f56c6c"
              @click="deleteContact(scope.row.contactId)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑联系人弹窗 -->
    <el-dialog 
      :title="dialogTitle" 
      :visible.sync="dialogVisible" 
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form 
        :model="contactForm" 
        :rules="formRules" 
        ref="contactForm" 
        label-width="100px"
        class="contact-form"
      >
        <el-form-item label="姓名" prop="contactName">
          <el-input v-model="contactForm.contactName"></el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="contactEmail">
          <el-input v-model="contactForm.contactEmail" placeholder="example@mail.com"></el-input>
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="contactForm.contactGroup" placeholder="请选择分组">
            <el-option label="朋友" value="朋友"></el-option>
            <el-option label="家人" value="家人"></el-option>
            <el-option label="同事" value="同事"></el-option>
            <el-option label="客户" value="客户"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="contactForm.phone"></el-input>
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="contactForm.remark" 
            type="textarea" 
            rows="3"
          ></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveContact">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import WriteLetter from './WriteLetter.vue';

export default {
  name: 'Contact',
  data() {
    return {
      // 基础状态
      loading: false,
      dialogVisible: false,
      isEdit: false,
      dialogTitle: '',
      searchKeyword: '',
      
      // 数据列表
      contactList: [],
      selectedContactIds: [],
      currentUserId: null,

      // 表单数据
      contactForm: {
        contactId: null,
        userId: null,
        contactName: '',
        contactEmail: '',
        contactGroup: '',
        phone: '',
        remark: '',
        isStar: 0
      },

      // 表单验证规则
      formRules: {
        contactName: [
          { required: true, message: '请输入联系人姓名', trigger: 'blur' }
        ],
        contactEmail: [
          { required: true, message: '请输入联系人邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    // 搜索过滤后的联系人列表
    filteredContacts() {
      if (!this.searchKeyword) return this.contactList
      const keyword = this.searchKeyword.toLowerCase()
      return this.contactList.filter(item => 
        item.contactName.toLowerCase().includes(keyword) || 
        item.contactEmail.toLowerCase().includes(keyword) ||
        item.phone.includes(keyword) ||
        (item.remark && item.remark.toLowerCase().includes(keyword))
      )
    }
  },
  mounted() {
    this.initUserInfo()
  },
  methods: {
    /**
     * 初始化用户信息（从sessionStorage获取当前用户ID）
     */
    initUserInfo() {
      const userInfoStr = sessionStorage.getItem('userInfo')
      if (!userInfoStr) {
        this.$router.push('/login')
        return
      }
      const userInfo = JSON.parse(userInfoStr)
      this.currentUserId = userInfo.userId
      if (this.currentUserId) {
        this.fetchContactList()
      } else {
        this.$message.error('获取用户信息失败，请重新登录')
        this.$router.push('/login')
      }
    },

    /**
     * 获取联系人列表
     */
    async fetchContactList() {
      this.loading = true
      try {
        const res = await this.$axios({
          url: 'http://localhost:8081/mail-contact/list',
          method: 'get',
          params: { userId: this.currentUserId }
        })
        if (res.data.code === '0') {
          this.contactList = res.data.data || []
        } else {
          this.$message.warning(res.data.msg || '获取联系人失败')
          this.contactList = []
        }
      } catch (err) {
        console.error('获取联系人异常:', err)
        this.$message.error('网络异常，获取联系人失败')
        this.contactList = []
      } finally {
        this.loading = false
      }
    },

    /**
     * 打开新增/编辑弹窗
     */
    openDialog(isEdit, row = null) {
      this.isEdit = isEdit
      this.dialogTitle = isEdit ? '编辑联系人' : '新增联系人'
      // 重置表单
      this.$refs.contactForm && this.$refs.contactForm.resetFields()
      
      if (isEdit && row) {
        // 编辑模式：填充表单数据
        this.contactForm = { ...row, userId: this.currentUserId }
      } else {
        // 新增模式：初始化表单
        this.contactForm = {
          contactId: null,
          userId: this.currentUserId,
          contactName: '',
          contactEmail: '',
          contactGroup: '',
          phone: '',
          remark: '',
          isStar: 0
        }
      }
      this.dialogVisible = true
    },

    /**
     * 保存联系人（新增/编辑）
     */
    async saveContact() {
      this.$refs.contactForm.validate(async (valid) => {
        if (valid) {
          try {
            const url = this.isEdit 
              ? 'http://localhost:8081/mail-contact/update' 
              : 'http://localhost:8081/mail-contact/save'
            
            const res = await this.$axios({
              url,
              method: 'post',
              headers: { 'Content-Type': 'application/json' },
              data: this.contactForm
            })

            if (res.data.code === '0') {
              this.$message.success(this.isEdit ? '编辑成功' : '新增成功')
              this.dialogVisible = false
              this.fetchContactList() // 刷新列表
            } else {
              this.$message.warning(res.data.msg || (this.isEdit ? '编辑失败' : '新增失败'))
            }
          } catch (err) {
            console.error('保存联系人异常:', err)
            this.$message.error('网络异常，操作失败')
          }
        }
      })
    },

    /**
     * 切换星标状态
     */
    async toggleStar(row) {
      try {
        const res = await this.$axios({
          url: 'http://localhost:8081/mail-contact/toggleStar',
          method: 'post',
          headers: { 'Content-Type': 'application/json' },
          data: {
            contactId: row.contactId,
            isStar: row.isStar === 1 ? 0 : 1
          }
        })
        if (res.data.code === '0') {
          row.isStar = row.isStar === 1 ? 0 : 1
          this.$message.success('操作成功')
        } else {
          this.$message.warning(res.data.msg || '操作失败')
        }
      } catch (err) {
        console.error('切换星标异常:', err)
        this.$message.error('网络异常，操作失败')
      }
    },

    async writeLetter(contactEmail) {
      this.$router.push({
        path: '/letter/write',
        query: { contactEmail }
      })
    },

    /**
     * 删除单个联系人
     */
    deleteContact(contactId) {
      this.$confirm('确定要删除该联系人吗？', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await this.$axios({
            url: 'http://localhost:8081/mail-contact/delete',
            method: 'post',
            headers: { 'Content-Type': 'application/json' },
            data: { contactId }
          })
          if (res.data.code === '0') {
            this.$message.success('删除成功')
            this.fetchContactList()
          } else {
            this.$message.warning(res.data.msg || '删除失败')
          }
        } catch (err) {
          console.error('删除联系人异常:', err)
          this.$message.error('网络异常，删除失败')
        }
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },

    /**
     * 批量删除联系人
     */
    batchDeleteContacts() {
      this.$confirm(`确定要删除选中的${this.selectedContactIds.length}个联系人吗？`, '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          const res = await this.$axios({
            url: 'http://localhost:8081/mail-contact/batchDelete',
            method: 'post',
            headers: { 'Content-Type': 'application/json' },
            data: { ids: this.selectedContactIds }
          })
          if (res.data.code === '0') {
            this.$message.success('批量删除成功')
            this.selectedContactIds = []
            this.fetchContactList()
          } else {
            this.$message.warning(res.data.msg || '批量删除失败')
          }
        } catch (err) {
          console.error('批量删除异常:', err)
          this.$message.error('网络异常，批量删除失败')
        }
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },
    

    /**
     * 处理表格选择事件
     */
    handleSelectionChange(val) {
      this.selectedContactIds = val.map(item => item.contactId)
    },

    /**
     * 搜索联系人
     */
    handleSearch() {
      // 输入防抖（可选）
      clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => {
        // 由computed属性自动处理过滤
      }, 300)
    }
  }
}
</script>

<style scoped>
.contact-wrapper {
  width: 100%;
  height: 100%;
  padding: 16px;
}

.contact-operate-bar {
  margin: 0 0 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.contact-list-card {
  height: calc(100% - 50px);
  overflow: hidden;
}

/* 加载状态样式 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #666;
}

.loading-icon {
  font-size: 28px;
  margin-bottom: 12px;
  animation: el-loading-rotate 1.5s linear infinite;
}

/* 无数据状态样式 */
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.add-btn {
  margin-top: 10px;
}

/* 表单样式 */
.contact-form {
  margin-top: 10px;
}
</style>