/**
 * 定义用户进行通信开发所需实现的接口。
 *
 *
 * <p>
 * 用户进行通信开发时需要实现该package中的接口，通常情况下仅需实现{@link org.smartboot.socket.Protocol}、{@link org.smartboot.socket.MessageProcessor}即可。
 * 如需仅需通讯层面的监控，smart-socket提供了接口{@link org.smartboot.socket.NetMonitor}以供使用。
 * </p>
 *
 * <p>
 * 完成本package的接口开发后，便可使用{@link org.smartboot.socket.transport.AioQuickClient}/{@link org.smartboot.socket.transport.AioQuickServer}提供AIO的客户端/服务端通信服务。
 * </p>
 *
 * @author 三刀
 * @version V1.0 , 2018/5/19
 */
package org.smartboot.socket;