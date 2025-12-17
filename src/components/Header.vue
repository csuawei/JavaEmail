<template>
    <div style="display: flex;line-height: 40px;">

    <div> 
        <el-input
        placeholder="搜索"
        prefix-icon="el-icon-search"
        v-model="input">
        </el-input>
    </div>
        <div>
            <el-dropdown>
            <span>{{ user.username }}</span>
            <i class="el-icon-arrow-down" style="margin-right: 15px;margin-left: 15px;"></i>
            <el-dropdown-menu slot="dropdown">
            <el-dropdown-item @click.native = "toUser">个人资料</el-dropdown-item>
            <el-dropdown-item @click.native = "logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
            </el-dropdown>
        </div>
         
    </div>
        
</template>

<script>


    export default{

        data(){
            return{
                user:JSON.parse(sessionStorage.getItem('userInfo')),
                input:''
            }
        },

        methods:{
            toUser(){
                console.log("to_user")
            },
            logout(){

                this.$confirm('确认退出登录','提示',{
                    confirmButtonText:'确定',
                    type:'warning',
                    center: true
                }).then(()=>{
                    this.$message({
                        type:'success',
                        message:'退出登录成功'
                    })
                    this.$router.push("/login")
                    sessionStorage.removeItem('userInfo');
                }).catch(()=>{
                    this.$message({
                        type:'info',
                        message:'取消'
                    })
                })

                
            }
        }
    }
</script>