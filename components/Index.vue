    <template>
        <el-container style="height: 100%; border: 1px solid #eee">
    <el-aside width="200px" style="background-color: rgb(238, 241, 246);height: 100%;">
        <Aside></Aside>
    </el-aside>
    
    <el-container style="height: 100%;">
        <el-header style="text-align: right; font-size: 12px;height: 100%;border-bottom: darkgray 2px solid;" >
            <Header></Header>
        </el-header>
        
        <el-main style="height: 100;">
            <router-view></router-view>
        </el-main>
    </el-container>
    </el-container>
    </template>

    <style>
    .el-header {
        
        color: #333;
        line-height: 60px;
    }
    
    .el-aside {
        color: #333;
    }
    </style>

    <script>
    import Aside from './Aside.vue';
    import Header from './Header.vue';
    import Main from './Main.vue';

    export default {
        components:{
            Aside,
            Header,
            Main
        },
    };
    </script>