<template>
  <div class="draft-write-container">
    <el-page-header @back="handleBack" content="编辑草稿"></el-page-header>

    <el-form ref="draftForm" :model="draftForm" class="draft-form" label-width="80px">
      <!-- 发件人下拉框 -->
      <el-form-item label="发件人" prop="senderEmail" :rules="[{ required: true, message: '请选择发件人邮箱', trigger: 'change' }]">
        <el-select
          v-model="draftForm.senderEmail"
          placeholder="请选择发件人邮箱"
          clearable
          style="width: 100%"
        >
          <template v-if="mailAccountList.length > 0">
            <el-option
              v-for="item in mailAccountList"
              :key="item.accountId"
              :label="item.email"
              :value="item.email"
            ></el-option>
          </template>
          <template v-else>
            <el-option disabled value="">暂无绑定的邮箱，请先绑定邮箱</el-option>
          </template>
        </el-select>
      </el-form-item>

      <!-- 收件人 -->
      <el-form-item label="收件人" prop="receiverEmail" :rules="[{ required: false, message: '请输入收件人邮箱，多个邮箱用逗号分隔', trigger: 'blur' }]">
        <el-input
          v-model="draftForm.receiverEmail"
          placeholder="请输入收件人邮箱，多个邮箱用逗号分隔"
          clearable
        ></el-input>
      </el-form-item>

      <!-- 主题 -->
      <el-form-item label="主题" prop="subject" :rules="[{ required: true, message: '请输入邮件主题', trigger: 'blur' }]">
        <el-input
          v-model="draftForm.subject"
          placeholder="请输入邮件主题"
          clearable
        ></el-input>
      </el-form-item>

      <!-- 正文 -->
      <el-form-item label="正文" prop="content" :rules="[{ required: false, message: '请输入邮件内容', trigger: 'blur' }]">
        <el-input
          v-model="draftForm.content"
          type="textarea"
          :rows="15"
          placeholder="请输入邮件内容"
        ></el-input>
      </el-form-item>

      <!-- 附件上传 -->
      <el-form-item label="附件">
        <el-upload
          class="upload-demo"
          action=""
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :file-list="fileList"
          :auto-upload="false"
          multiple
          :limit="5"
          :on-exceed="handleExceed"
        >
          <el-button size="small" type="primary">点击上传</el-button>
          <div slot="tip" class="el-upload__tip">支持多文件上传，最多5个文件</div>
        </el-upload>
      </el-form-item>

      <!-- 操作按钮 -->
      <el-form-item>
        <el-button type="primary" @click="handleSend">发送邮件</el-button>
        <el-button @click="handleBack">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  name: 'DraftWrite',
  data() {
    return {
      draftForm: {
        id: '', // 草稿ID
        senderEmail: '',
        receiverEmail: '',
        subject: '',
        content: '',
        saveTime: '' // 草稿保存时间
      },
      fileList: [], // 附件显示列表
      uploadFiles: [], // 实际待上传的文件数组
      mailAccountList: [], // 发件人邮箱列表
      currentUserId: null, // 当前用户ID
      draftId: '' // 路由传递的草稿ID
    }
  },
  created() {
    // 1. 获取路由参数中的草稿ID
    this.draftId = this.$route.query.draftId || ''
    // 2. 初始化用户信息
    this.initUserInfo()
  },
  methods: {
    /**
     * 初始化用户信息并加载基础数据
     */
    async initUserInfo() {
      try {
        const userInfoStr = sessionStorage.getItem('userInfo')
        if (!userInfoStr) {
          this.$message.error('未获取到用户信息，请重新登录')
          this.$router.push('/login')
          return
        }
        const userInfo = JSON.parse(userInfoStr)
        if (!userInfo.userId) {
          this.$message.error('用户信息不完整，请重新登录')
          this.$router.push('/login')
          return
        }
        this.currentUserId = userInfo.userId

        // 3. 加载发件人邮箱列表
        await this.getMailAccountList()

        // 4. 加载草稿数据
        await this.loadDraftData()
      } catch (err) {
        console.error('初始化失败：', err)
        this.$message.error('初始化异常，请重新登录')
        this.$router.push('/login')
      }
    },

    /**
     * 获取用户绑定的邮箱列表
     */
    async getMailAccountList() {
      try {
        const loadingInstance = this.$loading({
          lock: true,
          text: '正在获取邮箱列表...',
          background: 'rgba(255, 255, 255, 0.7)'
        })

        const res = await this.$axios({
          url: 'http://localhost:8081/mail-account/getAcById',
          method: 'post',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          data: { id: this.currentUserId }
        })

        loadingInstance.close()

        if (res.data && res.data.code === '0') {
          this.mailAccountList = res.data.data || []
          // 若草稿未设置发件人，默认选中第一个邮箱
          if (!this.draftForm.senderEmail && this.mailAccountList.length > 0) {
            this.draftForm.senderEmail = this.mailAccountList[0].email
          }
        } else {
          this.mailAccountList = []
          this.$message.warning(res.data?.msg || '未查询到绑定的邮箱')
        }
      } catch (err) {
        this.$loading().close()
        console.error('获取邮箱列表失败：', err)
        this.$message.error('获取邮箱列表失败，请稍后重试')
      }
    },

    /**
     * 加载已有草稿数据
     */
    async loadDraftData() {
      try {
        const loadingInstance = this.$loading({
          lock: true,
          text: '正在加载草稿...',
          background: 'rgba(255, 255, 255, 0.7)'
        })

        const res = await this.$axios({
          url: 'http://localhost:8081/mail-message/getDraftById',
          method: 'get',
          params: { id: this.draftId }
        })

        loadingInstance.close()

        if (res.data && res.data.code === '0') {
          const draft = res.data.data
          // 填充草稿数据到表单
          this.draftForm = {
            id: draft.id,
            senderEmail: draft.senderAccountEmail,
            receiverEmail: draft.senderEmail || '',
            subject: draft.subject,
            content: draft.content || '',
            saveTime: draft.saveTime
          }
          // 若有附件，需后端返回附件列表并渲染
        } else {
          this.$message.warning(res.data?.msg || '草稿数据加载失败')
          this.handleBack()
        }
      } catch (err) {
        this.$loading().close()
        console.error('加载草稿失败：', err)
        this.$message.error('加载草稿失败，请返回草稿箱重试')
        this.handleBack()
      }
    },

    /**
     * 处理附件添加
     */
    handleFileChange(file, fileList) {
      this.fileList = fileList
      this.uploadFiles = fileList.map(f => f.raw)
    },

    /**
     * 处理附件移除
     */
    handleFileRemove(file, fileList) {
      this.fileList = fileList
      this.uploadFiles = fileList.map(f => f.raw)
    },

    /**
     * 处理附件超出限制
     */
    handleExceed(files, fileList) {
      this.$message.warning(`最多只能上传5个文件，本次选择了${files.length}个，已超出${files.length + fileList.length - 5}个`)
    },

    /**
     * 发送邮件
     */
    async handleSend() {
      this.$refs.draftForm.validate(async (valid) => {
        if (!valid) return

        if (!this.draftForm.receiverEmail) {
          this.$message.warning('发送邮件前请填写收件人邮箱')
          return
        }
        if (!this.draftForm.content) {
          this.$message.warning('发送邮件前请填写邮件正文')
          return
        }

        try {
          const loadingInstance = this.$loading({
            lock: true,
            text: '正在发送邮件...',
            background: 'rgba(255, 255, 255, 0.7)'
          })

          // 构建发送数据（包含附件）
          const formData = new FormData()
          formData.append('senderEmail', this.draftForm.senderEmail.trim())
          formData.append('receiverEmail', this.draftForm.receiverEmail.trim())
          formData.append('subject', this.draftForm.subject.trim())
          formData.append('content', this.draftForm.content.trim())
          // 添加附件
          this.uploadFiles.forEach(file => {
            formData.append('attachments', file)
          })

          // 调用发送接口
          const res = await this.$axios({
            url: 'http://localhost:8081/mail-message/send',
            method: 'post',
            data: formData
          })

          loadingInstance.close()

          if (res.data.code === '0') {
            this.$message.success('邮件发送成功')
            this.$confirm('邮件发送成功，是否删除该草稿？', '提示', {
              type: 'info'
            }).then(async () => {
                await this.deleteDraft()
              this.handleBack()
            }).catch(() => {
              this.handleBack()
            })
          } else {
            this.$message.error(`发送失败：${res.data.msg || '未知错误'}`)
          }
        } catch (err) {
          this.$loading().close()
          console.error('发送邮件失败：', err)
          this.$message.error('网络异常，邮件发送失败')
        }
      })
    },

    /**
     * 删除草稿
     */
    async deleteDraft() {
      try {
        const res = await this.$axios({
          url: 'http://localhost:8081/mail-message/deleteById',
          method: 'get',
          params: { id: this.draftId }
        })
        if (res.data.code === '0') {
          this.$message.success('草稿已删除')
        }
      } catch (err) {
        console.error('删除草稿失败：', err)
        this.$message.warning('草稿删除失败')
      }
    },

    /**
     * 返回草稿箱
     */
    handleBack() {
      this.$router.push('/letter/draft') // 跳回草稿箱页面
    }
  }
}
</script>

<style scoped>
.draft-write-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.draft-form {
  margin-top: 20px;
  background: #fff;
  padding: 25px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.el-upload__tip {
  margin-left: 0;
  margin-top: 8px;
  color: #606266;
  font-size: 12px;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-button + .el-button {
  margin-left: 10px;
}
</style>