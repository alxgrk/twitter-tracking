package de.alxgrk.data

import org.apache.http.Header
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.BasicHttpEntity
import org.apache.http.message.BasicHeader
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpCoreContext
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.signer.Aws4Signer
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams
import software.amazon.awssdk.http.SdkHttpFullRequest
import software.amazon.awssdk.http.SdkHttpMethod
import software.amazon.awssdk.regions.Region
import java.io.ByteArrayInputStream
import java.net.URI

class AwsRequestSigningInterceptor(
    private val region: Region,
    private val service: String,
    private val signer: Aws4Signer,
    private val awsCredentialsProvider: AwsCredentialsProvider
) : HttpRequestInterceptor {

    override fun process(request: HttpRequest, context: HttpContext) {
        val uriBuilder = URIBuilder(request.requestLine.uri)

        // Copy Apache HttpRequest to an SDK request
        val sdkRequest = createSignableRequest(context, request, uriBuilder)

        // Sign it
        val credentials = awsCredentialsProvider.resolveCredentials()
        val signerParams = Aws4SignerParams.builder()
            .signingRegion(region)
            .signingName(service)
            .awsCredentials(credentials)
            .doubleUrlEncode(true)
            .build()
        val response = signer.sign(sdkRequest, signerParams)

        // Now copy everything back
        request.setHeaders(mapToHeaderList(response.headers()).toTypedArray())
        if (request is HttpEntityEnclosingRequest) {
            if (request.entity != null) {
                val basicHttpEntity = BasicHttpEntity()
                basicHttpEntity.content = response.contentStreamProvider().get().newStream()
                request.entity = basicHttpEntity
            }
        }
    }

    private fun createSignableRequest(
        context: HttpContext,
        request: HttpRequest,
        uriBuilder: URIBuilder
    ): SdkHttpFullRequest {
        val sdkHttpFullRequest = SdkHttpFullRequest.builder()

        (context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST) as? HttpHost)?.let {
            sdkHttpFullRequest.uri(URI.create(it.toURI()))
        }

        sdkHttpFullRequest.method(SdkHttpMethod.fromValue(request.requestLine.method))
        sdkHttpFullRequest.encodedPath(uriBuilder.build().rawPath)

        sdkHttpFullRequest.contentStreamProvider { ByteArrayInputStream(ByteArray(0)) }
        (request as? HttpEntityEnclosingRequest)?.let {
            it.entity?.content?.let { sdkHttpFullRequest.contentStreamProvider { it } }
        }

        uriBuilder.queryParams.forEach { sdkHttpFullRequest.appendRawQueryParameter(it.name, it.value) }
        request.allHeaders.filter { !skipHeader(it) }.forEach { sdkHttpFullRequest.appendHeader(it.name, it.value) }
        return sdkHttpFullRequest.build()
    }

    private fun mapToHeaderList(mapHeaders: MutableMap<String, MutableList<String>>): List<Header?> =
        mapHeaders.map { BasicHeader(it.key, it.value.joinToString(",")) }

    private fun skipHeader(header: Header): Boolean =
        (
            ("content-length".equals(
                header.name,
                ignoreCase = true
            ) && "0" == header.value) || // Strip Content-Length: 0
                "host".equals(header.name, ignoreCase = true)
            ) // Host comes from endpoint
}