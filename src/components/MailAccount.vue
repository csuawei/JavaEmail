<template>
  <div class="mail-account-container">
    <el-page-header @back="handleBack" content="邮箱账户管理"></el-page-header>

    <!-- 操作按钮区 -->
    <div class="operate-bar">
      <el-button type="primary" @click="showAddDialog = true">
        <i class="el-icon-plus"></i> 新增邮箱账户
      </el-button>
    </div>

    <!-- 账户列表 -->
    <el-card shadow="hover" class="account-list-card">
      <el-table
        :data="accountList"
        border
        fit
        style="width: 100%"
      >
        <el-table-column type="index" label="序号" width="60"></el-table-column>
        <el-table-column prop="email" label="邮箱地址" width="200"></el-table-column>
        <el-table-column label="SMTP服务器" width="200">
          <template slot-scope="scope">{{ scope.row.protocolSmtp }}:{{ scope.row.protocolSmtpPort }}</template>
        </el-table-column>
        
        <el-table-column label="POP3服务器" width="200">
          <template slot-scope="scope">{{ scope.row.protocolPop3 }}:{{ scope.row.protocolPop3Port }}</template>
        </el-table-column>

        <el-table-column label="IMAP服务器" width="200">
          <template slot-scope="scope">{{ scope.row.protocolImap }}:{{ scope.row.protocolImapPort }}</template>
        </el-table-column>


        
        <el-table-column label="操作" width="120">

          <template slot-scope="scope">
            <el-button
              type="text"
              size="mini"
              @click="changeAccount(scope.row.email)"
            >
              切换邮箱账户
            </el-button>
            <el-button
              type="text"
              size="mini"
              @click="deleteAccount(scope.row.accountId)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增账户弹窗 -->
    <el-dialog
      title="新增邮箱账户"
      :visible.sync="showAddDialog"
      width="500px"
    >
      <el-form ref="addForm" :model="addForm" label-width="100px" :rules="addFormRules">
        <el-form-item label="邮箱地址" prop="email">
          <el-input v-model="addForm.email" placeholder="请输入邮箱地址"></el-input>
        </el-form-item>
        <el-form-item label="授权码" prop="authCode">
          <el-input v-model="addForm.authCode" type="password" placeholder="请输入邮箱授权码"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAddForm">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'MailAccount',
  data() {
    return {
      accountList: [],
      showAddDialog: false,
      addForm: {
        email: '',
        authCode: ''
      },
      addFormRules: {
        email: [
          { required: true, message: '请输入邮箱地址', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
        ],
        authCode: [
          { required: true, message: '请输入授权码', trigger: 'blur' }
        ]
      },
      userId: ''
    }
  },
  mounted() {
    // 获取当前用户ID
    const userInfo = JSON.parse(sessionStorage.getItem('userInfo'))
    this.userId = userInfo.userId
    // 加载账户列表
    this.loadAccountList()
  },
  methods: {
    // 返回上一页
    handleBack() {
      this.$router.go(-1)
    },

    //
    // 加载账户列表
    async loadAccountList() {
      try {
        const res = await this.$axios({
          url: 'http://localhost:8081/mail-account/getAcById', 
          method: 'post',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          data: { id: this.userId } // 传递用户ID
        });
        if (res.data.code === '0') {
          this.accountList = res.data.data
        } else {
          this.$message.warning(res.data.msg || '获取账户列表失败')
        }
      } catch (err) {
        console.error('获取账户列表异常:', err)
        this.$message.error('网络异常，请稍后重试')
      }
    },


    // 删除账户
    async deleteAccount(accountId) {
      this.$confirm('确定要删除该邮箱账户吗？', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
            
          const res = await this.$axios.post('/mail-account/delete', {
            accountId: accountId
          })
          if (res.data.code === '0') {
            this.$message.success('删除成功')
            this.loadAccountList()
          } else {
            this.$message.warning(res.data.msg || '删除失败')
          }
        } catch (err) {
          console.error('删除账户异常:', err)
          this.$message.error('网络异常，请稍后重试')
        }
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },

    // 切换账户
    async changeAccount(email) {
      try {
        const res = await this.$axios({
          url: 'http://localhost:8081/mail-account/changeEmail', 
          method: 'post',
          data: {
            id: this.userId,
            email: email 
          } // 传递用户ID
        });

        if (res.data.code === '0') {
          this.$message.success('切换成功')
          // 更新相关数据
            sessionStorage.setItem("userInfo", JSON.stringify(res.data.data))

        } else {
          this.$message.warning(res.data.msg || '切换失败')
        }
      } catch (err) {
        console.error('切换账户异常:', err)
        this.$message.error('网络异常，请稍后重试')
      }
    },

    // 提交新增表单
    async submitAddForm() {
      this.$refs.addForm.validate(async (valid) => {
        if (valid) {
          try {
            const res = await this.$axios({
                url: 'http://localhost:8081/mail-account/addMailAccount',
                method: "post",                       // 请求方法
                data: {
                    id: this.userId,
                    email: this.addForm.email,
                    authCode: this.addForm.authCode
                }
            })
            if (res.data.code === '0') {
              this.$message.success('新增账户成功')
              this.showAddDialog = false
              this.addForm = { email: '', authCode: '' }
              this.loadAccountList()
            } else {
              this.$message.warning(res.data.msg || '新增失败')
            }
          } catch (err) {
            console.error('新增账户异常:', err)
            this.$message.error('网络异常，请稍后重试')
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.mail-account-container {
  padding: 16px;
}

.operate-bar {
  margin: 16px 0;
}

.account-list-card {
  margin-top: 16px;
}
</style>