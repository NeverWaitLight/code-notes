import { test, expect } from '@jest/globals'
import { getCpuArch, getGpuArch, getSystemInfo, getRecommendedPackage, isCpuArm64 } from './updater'

test('getCpuArch', () => {
  const cpuArch = getCpuArch()
  console.log(cpuArch)
  expect(cpuArch).toBeDefined()
})

test('isCpuArm64', () => {
  const isArm64 = isCpuArm64()
  console.log('CPU是ARM64架构:', isArm64)
  expect(typeof isArm64).toBe('boolean')
})

test('getGpuArch', async () => {
  const gpuInfo = await getGpuArch()
  expect(gpuInfo).toBeDefined()
})

test('getSystemInfo', async () => {
  const sysInfo = await getSystemInfo()
  console.log('系统信息:', sysInfo)
  expect(sysInfo).toHaveProperty('cpu')
  expect(sysInfo).toHaveProperty('gpuInfo')
  expect(sysInfo).toHaveProperty('windowsVersion')
})

test('getRecommendedPackage', async () => {
  const packages = await getRecommendedPackage()
  console.log('推荐安装包:', packages.find((pkg) => pkg.isRecommended)?.name)
  expect(Array.isArray(packages)).toBe(true)
  expect(packages.length).toBeGreaterThan(0)

  // 确保至少有一个包被推荐
  const hasRecommended = packages.some((pkg) => pkg.isRecommended)
  expect(hasRecommended).toBe(true)
})
