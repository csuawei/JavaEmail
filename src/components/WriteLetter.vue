<template>
  <div class="write-letter-container">
    <el-page-header @back="handleBack" content="写信"></el-page-header>

    <el-form ref="mailForm" :model="mailForm" class="mail-form" label-width="80px">
      <!-- 发件人下拉框 -->
      <el-form-item label="发件人" prop="senderEmail" :rules="[{ required: true, message: '请选择发件人邮箱', trigger: 'change' }]">
        <el-select
          v-model="mailForm.senderEmail"
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
      <el-form-item label="收件人" prop="receiverEmail" :rules="[{ required: true, message: '请输入收件人邮箱，多个邮箱用逗号分隔', trigger: 'blur' }]">
        <el-input
          v-model="mailForm.receiverEmail"
          placeholder="请输入收件人邮箱，多个邮箱用逗号分隔"
          clearable
        ></el-input>
      </el-form-item>

      <!-- 主题 -->
      <el-form-item label="主题" prop="subject" :rules="[{ required: true, message: '请输入邮件主题', trigger: 'blur' }]">
        <el-input
          v-model="mailForm.subject"
          placeholder="请输入邮件主题"
          clearable
        ></el-input>
      </el-form-item>

      <!-- 正文 -->
      <el-form-item label="正文" prop="content" :rules="[{ required: true, message: '请输入邮件内容', trigger: 'blur' }]">
        <el-input
          v-model="mailForm.content"
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
        <el-button type="primary" @click="handleSend">发送</el-button>
        <el-button @click="handleSaveDraft">保存草稿</el-button>
        <el-button @click="handleBack">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
export default {
  data() {
    return {
      mailForm: {
        senderEmail: '', // 选中的发件人邮箱
        receiverEmail: '',
        subject: '',
        content: ''
      },
      fileList: [], // 附件显示列表
      uploadFiles: [], // 实际待上传的文件数组
      mailAccountList: [], // 后端返回的邮箱列表
      currentUserId: null, // 当前用户ID
      contactEmail: '' // 联系人邮箱
    }
  },
  created() {

    this.contactEmail = this.$route.query.contactEmail || ''
    if (this.$route.query.contactEmail) {
      this.mailForm.receiverEmail = this.$route.query.contactEmail
    }
    // 1. 从sessionStorage获取用户信息（增加异常处理）
    try {
      const userInfoStr = sessionStorage.getItem('userInfo');
      if (!userInfoStr) {
        this.$message.error('未获取到用户信息，请重新登录');
        this.$router.push('/login');
        return;
      }
      const userInfo = JSON.parse(userInfoStr);
      // 检查userId是否存在
      if (!userInfo.userId) {
        this.$message.error('用户信息不完整，请重新登录');
        this.$router.push('/login');
        return;
      }
      this.currentUserId = userInfo.userId;
      // 2. 调用接口获取邮箱列表
      this.getMailAccountList();
    } catch (err) {
      console.error('解析用户信息失败：', err);
      this.$message.error('用户信息异常，请重新登录');
      this.$router.push('/login');
    }
  },
  methods: {
    /**
     * 获取用户绑定的邮箱列表
     */
    async getMailAccountList() {
      try {
        const res = await this.$axios({
          url: 'http://localhost:8081/mail-account/getAcById', 
          method: 'post',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          data: { id: this.currentUserId } // 传递用户ID
        });

        this.$message.closeAll();

        // 适配后端ResultUtil返回格式（code=0为成功）
        if (res.data && res.data.code === '0') {
          this.mailAccountList = res.data.data || [];
          // 自动选中第一个邮箱（如果有）
          if (this.mailAccountList.length > 0) {
            this.mailForm.senderEmail = this.mailAccountList[0].email;
          }
        } else {
          this.mailAccountList = [];
          this.$message.warning(res.data?.msg || '未查询到绑定的邮箱');
        }
      } catch (err) {
        this.$message.closeAll();
        console.error('获取邮箱列表失败：', err);
        if (err.response) {
          this.$message.error(`获取邮箱失败：${err.response.status}（${err.response.data?.msg || '服务器错误'}）`);
        } else {
          this.$message.error('网络异常，无法连接到服务器');
        }
      }
    },

    /**
     * 处理附件添加
     */
    handleFileChange(file, fileList) {
      this.fileList = fileList;
      this.uploadFiles = fileList.map(f => f.raw); // 提取原始文件对象
    },

    /**
     * 处理附件移除
     */
    handleFileRemove(file, fileList) {
      this.fileList = fileList;
      this.uploadFiles = fileList.map(f => f.raw);
    },

    /**
     * 处理附件超出限制
     */
    handleExceed(files, fileList) {
      this.$message.warning(`最多只能上传5个文件，本次选择了${files.length}个，已超出${files.length + fileList.length - 5}个`);
    },

    /**
     * 发送邮件
     */
    async handleSend() {
      this.$refs.mailForm.validate(async (valid) => {
        if (valid) {
          try {
            const formData = new FormData();
            if(this.contactEmail) {
              this.mailForm.receiverEmail = this.contactEmail
            }
            // 添加文本参数
            formData.append('senderEmail', this.mailForm.senderEmail.trim());
            formData.append('receiverEmail', this.mailForm.receiverEmail.trim());
            formData.append('subject', this.mailForm.subject.trim());
            formData.append('content', this.mailForm.content.trim());
            // 添加附件
            this.uploadFiles.forEach(file => {
              formData.append('attachments', file);
            });

            const res = await this.$axios({
              url: 'http://localhost:8081/mail-message/send',
              method: 'post',
              data: formData // 自动处理multipart/form-data格式
            });

            if (res.data.code === '0') {
              this.$message.success('邮件发送成功！');
              this.handleBack(); // 返回上一页
            } else {
              this.$message.error(`发送失败：${res.data.msg || '未知错误'}`);
            }
          } catch (err) {
            console.error('发送邮件失败：', err);
            if (err.response) {
              this.$message.error(`发送失败：${err.response.status}（${err.response.data?.msg || '服务器错误'}）`);
            } else {
              this.$message.error('网络异常，发送失败');
            }
          }
        }
      });
    },

    /**
     * 保存草稿
     */
    async handleSaveDraft() {
      // 至少填写发件人，收件人和主题
      if (!this.mailForm.senderEmail) {
        this.$message.warning('请选择发件人邮箱');
        return;
      }
      
      if (!this.mailForm.receiverEmail) {
        this.$message.warning('请选择收件人邮箱');
        return;
      }

      if (!this.mailForm.subject) {
        this.$message.warning('请输入邮件主题');
        return;
      }

      try {
        const formData = new FormData();
        formData.append('senderEmail', this.mailForm.senderEmail.trim());
        formData.append('receiverEmail', this.mailForm.receiverEmail?.trim() || '');
        formData.append('subject', this.mailForm.subject.trim());
        formData.append('content', this.mailForm.content?.trim() || '');

        const res = await this.$axios({
          url: 'http://localhost:8081/mail-message/saveDraft', 
          method: 'post',
          data: formData
        });

        if (res.data.code === '0') {
          this.$message.success('草稿保存成功！');
        } else {
          this.$message.error(`保存失败：${res.data.msg || '未知错误'}`);
        }
      } catch (err) {
        console.error('保存草稿失败：', err);
        this.$message.error('网络异常，保存草稿失败');
      }
    },

    /**
     * 返回上一页
     */
    handleBack() {
      this.$router.back();
    }
  }
}
</script>

<style scoped>
.write-letter-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px); /* 占满剩余高度 */
}

.mail-form {
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