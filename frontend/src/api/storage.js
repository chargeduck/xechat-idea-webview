import request from '@/utils/request'

/**
 * 云存档四件套（均需登录，satoken 头由 request 拦截器注入）
 */

/** 保存存档：SaveDTO(gameInfoId, saveName, saveData, version) */
export function storageSave(data) {
  return request({
    url: '/storage/save',
    method: 'post',
    data
  })
}

/** 读取存档：StorageLoadQuery(gameInfoId, saveName) */
export function storageLoad(data) {
  return request({
    url: '/storage/load',
    method: 'post',
    data
  })
}

/** 存档槽位列表：GET /storage/slots/{gameInfoId} */
export function storageSlots(gameInfoId) {
  return request({
    url: `/storage/slots/${gameInfoId}`,
    method: 'get'
  })
}

/** 删除存档：StorageLoadQuery(gameInfoId, saveName) */
export function storageDelete(data) {
  return request({
    url: '/storage/delete',
    method: 'post',
    data
  })
}
