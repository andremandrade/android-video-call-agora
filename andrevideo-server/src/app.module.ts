import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { TokenController } from './token/token.controller';

@Module({
  imports: [],
  controllers: [AppController, TokenController],
  providers: [AppService],
})
export class AppModule {}
