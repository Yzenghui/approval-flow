const { execSync } = require('child_process');

const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  red: '\x1b[31m',
  cyan: '\x1b[36m'
};

function log(message, color = '') {
  console.log(`${color}${message}${colors.reset}`);
}

function killPort(port) {
  try {
    const command = process.platform === 'win32'
      ? `for /f "tokens=5" %a in ('netstat -ano ^| findstr :${port} ^| findstr LISTENING') do taskkill /F /PID %a 2>nul`
      : `lsof -ti:${port} | xargs kill -9 2>/dev/null`;
    
    execSync(command, { shell: true });
    return true;
  } catch {
    return false;
  }
}

function main() {
  log('============================================', colors.cyan);
  log('  审批流系统 - 停止脚本', colors.cyan);
  log('============================================', colors.cyan);
  log('');

  log('[停止] 正在停止后端服务 (端口 8080)...', colors.yellow);
  if (killPort(8080)) {
    log('[✓] 后端服务已停止', colors.green);
  } else {
    log('[提示] 端口 8080 没有被占用', colors.yellow);
  }

  log('[停止] 正在停止前端服务 (端口 5173)...', colors.yellow);
  if (killPort(5173)) {
    log('[✓] 前端服务已停止', colors.green);
  } else {
    log('[提示] 端口 5173 没有被占用', colors.yellow);
  }

  log('');
  log('============================================', colors.cyan);
  log('  所有服务已停止', colors.green);
  log('============================================', colors.cyan);
}

main();
