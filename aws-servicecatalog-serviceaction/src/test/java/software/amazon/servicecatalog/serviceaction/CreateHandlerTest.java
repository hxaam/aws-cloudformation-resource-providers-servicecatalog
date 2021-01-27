package software.amazon.servicecatalog.serviceaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.List;

import com.google.common.collect.ImmutableList;
import software.amazon.awssdk.services.servicecatalog.model.CreateServiceActionResponse;
import software.amazon.awssdk.services.servicecatalog.model.InvalidParametersException;
import software.amazon.awssdk.services.servicecatalog.model.LimitExceededException;
import software.amazon.awssdk.services.servicecatalog.model.ServiceActionDetail;
import software.amazon.awssdk.services.servicecatalog.model.ServiceActionSummary;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    final static private String INVALID_PARAMETERS_EXCEPTION = "invalid parameters exception";
    final static private String LIMIT_EXCEED_EXCEPTION = "Action service limit exceeded";

    private CreateHandler handler;
    private ResourceModel model;


    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        model = ResourceModel.builder()
                .name("StartEC2Instance")
                .definitionType("SSM_AUTOMATION")
                .definition(buildDefinitionParameters())
                .description("Start EC2 Instances")
                .build();
    }

    private ServiceActionSummary buildServiceActionSummary() {
        return ServiceActionSummary.builder()
                .definitionType("SSM_AUTOMATION")
                .description("Start EC2 Instances")
                .id("act-1993jive")
                .name("StartEC2Instance")
                .build();
    }

    private List<DefinitionParameter> buildDefinitionParameters() {
        return ImmutableList.of(
                DefinitionParameter.builder().key("Name").value("AWS-StartEC2Instances").build(),
                DefinitionParameter.builder().key("Version").value("1").build(),
                DefinitionParameter.builder().key("AssumeRole").value("arn:aws:iam::123456789012:role/role").build(),
                DefinitionParameter.builder().key("Parameters").value("[{\\\"Name\\\":\\\"InstanceId\\\",\\\"Type\\\":\\\"TARGET\\\"}]").build()
        );
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ServiceActionSummary serviceActionSummary = buildServiceActionSummary();
        final ServiceActionDetail serviceActionDetail = ServiceActionDetail
                .builder()
                .serviceActionSummary(serviceActionSummary)
                .build();
        final CreateServiceActionResponse result = CreateServiceActionResponse
                .builder()
                .serviceActionDetail(serviceActionDetail)
                .build();
        doReturn(result).when(proxy).injectCredentialsAndInvokeV2(
                ArgumentMatchers.any(),
                ArgumentMatchers.any());

        final ResourceHandlerRequest<ResourceModel> resourceHandlerRequest = ResourceHandlerRequest
                .<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("token")
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, resourceHandlerRequest, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isZero();
        assertThat(response.getResourceModel().getId()).isEqualTo("act-1993jive");
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_WhenInvalidParameters_Exception() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest
                .<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("token")
                .build();
        doThrow(InvalidParametersException.builder().message(INVALID_PARAMETERS_EXCEPTION).build())
                .when(proxy).injectCredentialsAndInvokeV2(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any());

        // When
        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_WhenLimitExceeded_Exception() {
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest
                .<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("token")
                .build();
        doThrow(LimitExceededException.builder().message(LIMIT_EXCEED_EXCEPTION).build())
                .when(proxy).injectCredentialsAndInvokeV2(
                ArgumentMatchers.any(),
                ArgumentMatchers.any());

        // When
        assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }
}
