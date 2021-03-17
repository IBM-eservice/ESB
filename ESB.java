CREATE COMPUTE MODULE BalConvertToCBS
	CREATE FUNCTION Main() RETURNS BOOLEAN
	BEGIN

		SET Environment.variables.gbl = InputRoot.XMLNSC.Request.UUID;
		DECLARE xsi NAMESPACE 'http://www.w3.org/2001/XMLSchema-instance';
		SET OutputRoot.XMLNSC.FIXML.(XMLNSC.Attribute)xsi:schemaLocation ='http://www.finacle.com/fixml  executeFinacleScript.xsd';
		SET OutputRoot.XMLNSC.FIXML.Header.RequestHeader.MessageKey.RequestUUID = Environment.variables.gbl;
		DECLARE outputRefHdr REFERENCE TO OutputRoot.XMLNSC.FIXML.Header.RequestHeader ;
		SET outputRefHdr.MessageKey.ServiceRequestId = 'executeFinacleScript' ;
		SET outputRefHdr.MessageKey.ServiceRequestVersion = '10.2';
		SET outputRefHdr.MessageKey.ChannelId = InputRoot.XMLNSC.Request.ChannelId;
		SET outputRefHdr.MessageKey.LanguageId = '';
		SET outputRefHdr.RequestMessageInfo.BankId = '' ;
		SET outputRefHdr.RequestMessageInfo.TimeZone = '';
		SET outputRefHdr.RequestMessageInfo.EntityId = '';
		SET outputRefHdr.RequestMessageInfo.EntityType = '';
		SET outputRefHdr.RequestMessageInfo.ArmCorrelationId ='';
		SET outputRefHdr.RequestMessageInfo.MessageDateTime = CURRENT_TIMESTAMP ;
		SET outputRefHdr.Reversal.ParentRequestUUID = '';
		SET outputRefHdr.Security.Token.PasswordToken.UserId = '';
		SET outputRefHdr.Security.Token.PasswordToken.Password = '';
		SET outputRefHdr.Security.FICertToken = '';
		SET outputRefHdr.Security.RealUserLoginSessionId = '';
		SET outputRefHdr.Security.RealUser = '';
		SET outputRefHdr.Security.RealUserPwd = '';
		SET outputRefHdr.Security.SSOTransferToken = '';
		SET outputRefHdr.CustomInfo.table.key = '';
		SET outputRefHdr.CustomInfo.table.value ='';
		SET OutputRoot.XMLNSC.FIXML.Body.executeFinacleScriptRequest.ExecuteFinacleScriptInputVO.requestId = 'IBL0653AccBalInqmn001.scr';
		SET OutputRoot.XMLNSC.FIXML.Body.executeFinacleScriptRequest.executeFinacleScript_CustomData.Foracid = InputRoot.XMLNSC.Request.accountNumber;
		RETURN TRUE;
	END;
END MODULE;
CREATE COMPUTE MODULE BalCBSToResp
	CREATE FUNCTION Main() RETURNS BOOLEAN
	BEGIN
		DECLARE i INTEGER 1 ;
		DECLARE count INTEGER;
		DECLARE inputRefResBody ROW InputRoot.XMLNSC.FIXML.Body.executeFinacleScriptResponse.executeFinacleScript_CustomData;
		IF InputRoot.XMLNSC.FIXML.Header.ResponseHeader.HostTransaction.Status = 'SUCCESS' THEN

			IF inputRefResBody.sucFaiFlag = 'F'THEN
				SET OutputRoot.JSON.Data.Header.Status = 'FAILURE';
				SET OutputRoot.JSON.Data.Header.Message = inputRefResBody.message;

			ELSE

				SET OutputRoot.JSON.Data.Header.Status = InputRoot.XMLNSC.FIXML.Header.ResponseHeader.HostTransaction.Status;
				SET OutputRoot.JSON.Data.Header.Message = InputRoot.XMLNSC.FIXML.Header.ResponseHeader.HostTransaction.Status;

				SET OutputRoot.JSON.Data.Body.accountNumber 			= inputRefResBody.acctDtls.acctNum;
				SET OutputRoot.JSON.Data.Body.status 					= inputRefResBody.acctDtls.acctStatus;
				SET OutputRoot.JSON.Data.Body.openedOn 					= inputRefResBody.acctDtls.acct_opn_date;
				SET OutputRoot.JSON.Data.Body.closedOn 					= inputRefResBody.acctDtls.acct_cls_date;
				SET OutputRoot.JSON.Data.Body.currency 					= inputRefResBody.acctDtls.acctCrncyCode;
				SET OutputRoot.JSON.Data.Body.name 						= inputRefResBody.acctDtls.acctName;
				SET OutputRoot.JSON.Data.Body.customerId 				= inputRefResBody.acctDtls.CifId;
				SET OutputRoot.JSON.Data.Body.accountType 				= 'ESC';
				SET OutputRoot.JSON.Data.Body.balances.informational 	= inputRefResBody.acctDtls.EffAvailableAmt ; 
				SET OutputRoot.JSON.Data.Body.balances.available 		= inputRefResBody.acctDtls.AvilBal ;
				SET OutputRoot.JSON.Data.Body.frezcode 					= inputRefResBody.acctDtls.Frez_Code;
				SET OutputRoot.JSON.Data.Body.systemReservedAmt 		= inputRefResBody.acctDtls.systemReservedAmt;
					
			END IF ;
		ELSE
			IF EXISTS(InputRoot.XMLNSC.FIXML.Body.Error[]) THEN
				SET OutputRoot.JSON.Data.Header.Status = InputRoot.XMLNSC.FIXML.Header.ResponseHeader.HostTransaction.Status;
				IF EXISTS (InputRoot.XMLNSC.FIXML.Body.Error.FIBusinessException[]) THEN

					SET OutputRoot.JSON.Data.Header.Message = InputRoot.XMLNSC.FIXML.Body.Error.FIBusinessException.ErrorDetail[i].ErrorDesc;

				ELSE
					SET OutputRoot.JSON.Data.Header.Message = InputRoot.XMLNSC.FIXML.Body.Error.FISystemException.ErrorDetail[i].ErrorDesc;

				END IF ;
			ELSE
				SET OutputRoot.JSON.Data.Header.Status = InputRoot.XMLNSC.FIXML.Header.ResponseHeader.HostTransaction.Status;
				SET OutputRoot.JSON.Data.Header.Message= 'FAILURE';
			END IF ;
		END IF ;


		RETURN TRUE;
	END;

END MODULE;

CREATE COMPUTE MODULE BalErrorHandling
	CREATE FUNCTION Main() RETURNS BOOLEAN
	BEGIN

	DECLARE exceptionMessage CHARACTER Common.ParseExcepListToText(InputExceptionList);
		SET OutputRoot.XMLNSC.Response.Header.status 		=  'FAILURE';
			SET OutputRoot.XMLNSC.Response.Header.status 	=  'FAILURE';
			SET OutputRoot.XMLNSC.Response.Header.errorCode = 'Error';
			SET OutputRoot.XMLNSC.Response.Header.errorDesc = exceptionMessage;
			PROPAGATE TO TERMINAL 'out';
			SET OutputRoot.XMLNSC.Event.LogDetails.URN 			= COALESCE(Environment.variables.gbl,'unknown');
		SET OutputRoot.XMLNSC.Event.LogDetails.DATETIME 	= CAST(CURRENT_TIMESTAMP AS CHARACTER FORMAT 'yyyy-MM-dd HH:mm:ss.SSS');
		SET OutputRoot.XMLNSC.Event.LogDetails.SERVICENAME  = ExecutionGroupLabel ||'-' || MessageFlowLabel;
		SET OutputRoot.XMLNSC.Event.LogDetails.Payload		= InputExceptionList;
		PROPAGATE TO TERMINAL 'out1';
		RETURN FALSE;
	END;
 END MODULE;

