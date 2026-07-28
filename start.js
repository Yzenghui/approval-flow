const { execSync, spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  red: '\x1b[31m',
  cyan: '\x1b[36m'
};

const projectDir = __dirname;
const backendDir = path.join(projectDir, 'backend');
const frontendDir = path.join(projectDir, 'frontend');

function log(message, color = '') {
  console.log(`${color}${message}${colors.reset}`);
}

function checkPrerequisites() {
  log('============================================', colors.cyan);
  log('  审批流系统 - 一键启动脚本', colors.cyan);
  log('============================================', colors.cyan);
  log('');

  try {
    const javaVersion = execSync('java -version 2>&1', { encoding: 'utf8' });
    log(`[✓] Java 环境: ${javaVersion.split('\n')[0].replace(/.*version\s+/, '')}`, colors.green);
  } catch {
    log('[✗] 未检测到 Java，请先安装 JDK 21', colors.red);
    process.exit(1);
  }

  try {
    const nodeVersion = execSync('node -v', { encoding: 'utf8' }).trim();
    log(`[✓] Node.js 环境: ${nodeVersion}`, colors.green);
  } catch {
    log('[✗] 未检测到 Node.js，请先安装 Node.js 20+', colors.red);
    process.exit(1);
  }

  log('');
}

function installFrontendDeps() {
  const nodeModulesPath = path.join(frontendDir, 'node_modules');
  if (!fs.existsSync(nodeModulesPath)) {
    log('[提示] 首次运行，正在安装前端依赖...', colors.yellow);
    log('');
    try {
      execSync('npm install', { cwd: frontendDir, stdio: 'inherit' });
      log('[✓] 前端依赖安装完成', colors.green);
    } catch (e) {
      log('[✗] 前端依赖安装失败，请检查网络', colors.red);
      process.exit(1);
    }
  }
}

function startBackend() {
  log('[启动] 正在启动后端服务 (Spring Boot)...', colors.blue);
  log('       后端地址: http://localhost:8080', colors.cyan);
  
  const mvnwPath = path.join(backendDir, 'mvnw.cmd');
  const command = process.platform === 'win32' ? mvnwPath : './mvnw';
  
  const backendProcess = spawn(command, ['spring-boot:run'], {
    cwd: backendDir,
    stdio: 'inherit',
    shell: true
  });

  backendProcess.on('error', (err) => {
    log(`[错误] 后端启动失败: ${err.message}`, colors.red);
  });

  return backendProcess;
}

function startFrontend() {
  setTimeout(() => {
    log('[启动] 正在启动前端服务 (Vite)...', colors.blue);
    log('       前端地址: http://localhost:5173', colors.cyan);

    const npmCmd = process.platform === 'win32' ? 'npm.cmd' : 'npm';
    
    const frontendProcess = spawn(npmCmd, ['run', 'dev'], {
      cwd: frontendDir,
      stdio: 'inherit',
      shell: true
    });

    frontendProcess.on('error', (err) => {
      log(`[错误] 前端启动失败: ${err.message}`, colors.red);
    });

  }, 15000);
}

function main() {
  checkPrerequisites();
  installFrontendDeps();

  log('');
  log('[等待] 正在等待后端服务启动 (约 15 秒)...', colors.yellow);
  log('');

  const backendProcess = startBackend();
  startFrontend();

  log('');
  log('============================================', colors.cyan);
  log('  服务启动指令已发送！', colors.green);
  log('============================================', colors.cyan);
  log(`  前端地址: ${colors.green}http://localhost:5173${colors.cyan}`);
  log(`  后端地址: ${colors.green}http://localhost:8080${colors.cyan}`);
  log('');
  log('  测试账号:');
  log('    管理员: admin / 123456');
  log('    员工:   zhangsan / 123456');
  log('============================================', colors.cyan);
  log('');
  log('[提示] 要停止服务，请运行: node stop.js', colors.yellow);
  log('');

  // 保持主进程运行
  process.on('SIGINT', () => {
    log('\n[信息] 正在停止所有服务...', colors.yellow);
    backendProcess.kill();
    process.exit(0);
  });

  // 保持进程不退出
  setInterval(() => {}, 1000);
}

main();
