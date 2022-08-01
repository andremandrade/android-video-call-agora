import { Controller, Get, HttpException, HttpStatus, Query, Req } from '@nestjs/common';
import {RtcRole, RtcTokenBuilder} from 'agora-access-token';

@Controller('token')
export class TokenController {

    // Fill the appID and appCertificate key given by Agora.io
    appID = "";
    appCertificate = "";

    // token expire time, hardcode to 3600 seconds = 1 hour
    expirationTimeInSeconds = 3600;
    role = RtcRole.PUBLISHER;

    @Get()
    getToken(@Query('channel') channelName) : TokenResponse  {
        let currentTimestamp = Math.floor(Date.now() / 1000)
        let privilegeExpiredTs = currentTimestamp + this.expirationTimeInSeconds
        
        // use 0 if uid is not specified
        let uid = 0
        if (!channelName) {
            throw new HttpException('channel name is required', HttpStatus.BAD_REQUEST);
        }
    
        var key = RtcTokenBuilder.buildTokenWithUid(this.appID, this.appCertificate, channelName, uid, this.role, privilegeExpiredTs);
            
        return {key: key};
    }
}

interface TokenResponse {
    key: string
}
