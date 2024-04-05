import React, { Component,PropTypes } from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as userActions from '../../../../redux/actions/userActions';
import * as groupActions from '../../../../redux/actions/groupActions';
import * as groupsActions from '../../../../redux/actions/groupsActions';
import * as commentActions from '../../../../redux/actions/commentActions';
import ApiHandler from '../../../../core/ApiHandler';
import groupUtiles from '../../../../core/groupUtiles';
import './style.css';
import _ from 'lodash';
import InputSendComponent from './Components/InputSendComponent';
import TopBar from './Components/TopBar';
import Comments from './Components/Comments';
import LiveCommentProto from '../../../../core/LiveCommentProto';
import messageUtiles from '../../../../core/messageUtiles';
import moment from 'moment';

class ChatComponent extends Component {

  constructor(props){
    super(props);

    this.state={
      currentGroup: {},
      comments: [],
      savedGroups: {},
      groupId: props.groupId,
      socketUrl:{}
    }

    this.onClickJoinCircle = this.onClickJoinCircle.bind(this);
    this.appendComment = this.appendComment.bind(this);

  }

  scrollToBottom() {
    let node = ReactDOM.findDOMNode(this.messagesContainer);
    const scrollHeight = node.scrollHeight;
    const height = node.clientHeight;
    const maxScrollTop = node ? scrollHeight + height : scrollHeight - height;
    node.scrollTop = maxScrollTop > 0 ? maxScrollTop : 0;
    this.isScrollAtBottom = true;
    if(node.style.paddingBottom !== ''){
      node.style.paddingBottom = '0px';
    }
  }

  componentWillReceiveProps(nextProps){
    if (this.props.currentGroup !== nextProps.currentGroup) {
        this.setState({currentGroup: nextProps.currentGroup, rows:1});

        if(nextProps.currentGroup.discoverable){
          if(nextProps.currentGroup.membershipState === 'ACTIVE' || nextProps.currentGroup.preapprove === 'ANYONE'){
            this.loadGroupComments(nextProps.currentGroup.uuid);
            if(this.connection){
              this.connection.close();
            }
          }
        } else {
          if(nextProps.currentGroup.membershipState === 'ACTIVE'){
            this.loadGroupComments(nextProps.currentGroup.uuid);
            if(this.connection){
              this.connection.close();
            }
          }
        }
    }

    //Check if the Props for group comments have in fact changed.
    if (this.props.comments !== nextProps.comments) {
        this.setState({comments: nextProps.comments });
    }
    //Check if the Props for group comments have in fact changed.
    if (this.props.currentUser !== nextProps.currentUser) {
        this.setState({currentUser: nextProps.currentUser });
    }

    if(this.props.socketUrl !== nextProps.socketUrl){
        this.setState({socketUrl: nextProps.socketUrl});
    }
  }


 appendComment(newChat,hasImage,clientHeight){
   this.setState({comments:[...this.state.comments, newChat]});

   let node = ReactDOM.findDOMNode(this.messagesContainer),
      isFristCommentOfDay = this.state.comments.length < 2 ? false :  moment.unix(newChat.creationTime/1000).format("DD M") !==
      moment.unix(this.state.comments[this.state.comments.length -1].creationTime/1000).format("DD M");

     if(hasImage === 'true'){
       if(isFristCommentOfDay){
          node.style.paddingBottom = `${clientHeight + 80 + 350}px`;
       } else {
         node.style.paddingBottom = `${clientHeight + 80 +250}px`;
       }

     } else {
       if (clientHeight < 80){
         clientHeight = 80;
       }
       if(isFristCommentOfDay){
          node.style.paddingBottom = `${(clientHeight+80)+100}px`;
       } else {
       node.style.paddingBottom = `${clientHeight+80}px`;
     }
    }

   node.style.height = 'calc(100vh - 240px)';
 }

  loadGroupComments(uuid){

    let request = ApiHandler.attachHeader({groupId:uuid ? uuid : this.state.currentGroup.uuid});
    this.props.actions.loadRequestedGroupChats(request).then( response => {
      if(response){
         this.setState({comments: response});
          this.scrollToBottom();
      }
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  adjustHeightWithPhoto(height){
    let node = ReactDOM.findDOMNode(this.messagesContainer);

    node.style.height = height;
    this.scrollToBottom()
  }

  componentDidUpdate() {

    if(!_.isEmpty(this.state.socketUrl)){
      if(!this.connection || (this.connection && this.connection.readyState === 3)){
        this.connectToLiveServer();
      }
    }
  }

    connectToLiveServer(){

      let message = LiveCommentProto.Present.LiveCommentsRequest.create({
                  groupId: this.props.currentGroup.uuid,
                  userId: this.props.currentUser.id,
                  version:1,
                  header:{
                    clientUuid: localStorage.getItem('clientUuid'),
                    requestUuid: ApiHandler.guid(),
                    authorizationKey: "not implemented",
                    spaceId: "everyone",
                    apiVersion: "0",
                    platform: 3
                  }
        }),
        buffer  = LiveCommentProto.Present.LiveCommentsRequest.encode(message).finish(),
        that = this;

      this.connection = new WebSocket(this.state.socketUrl);
      this.connection.binaryType = "arraybuffer";

      //when connection is open send the message
      this.connection.onopen = event => {
          this.connection.send(buffer.buffer.slice(buffer.byteOffset, buffer.byteOffset + buffer.length));
      }

      //WHEN WE GET BACK THE RESPONSE DOES THIS
      this.connection.onmessage = event => {

        if(!_.isEmpty(LiveCommentProto.Present.CommentResponse.decode(new Uint8Array(event.data)))) {
          let comment = LiveCommentProto.Present.CommentResponse.decode(new Uint8Array(event.data));
          let newComments = that.state.comments;
          if(!comment.deleted){
            that.setState({comments: [...that.state.comments, comment]});
          } else {
            let index = messageUtiles.findIndex(newComments, comment.uuid);
            newComments.splice(index,1)
            that.setState({comments:newComments});
          }
        }

        that.loadGroupComments();

      }
    }

    loadUserSavedGroups(){
      let requestTest = ApiHandler.attachHeader({
        location:this.props.currentUser.home.location
      }),
          that= this;

      this.props.actions.getSavedGroups(requestTest).then( response => {
        this.setState({
          savedGroups: response,
          muted:groupUtiles.isGroupMuted(response.mutedGroups,that.props.currentGroup)
        });

      }).catch(error => {
        const errorMessage = ApiHandler.getErrorMessage(error);
            console.log(errorMessage);
      });
    }

    //check if user has the group or not, if not add the join button
    onClickJoinCircle(){
      let request = ApiHandler.attachHeader({groupId:this.props.currentGroup.uuid})
      this.props.actions.saveGroupRequest(request).then(response => {
        this.setState({joined: true,showSetting:true});
        this.loadUserSavedGroups();
        this.props.callJoinButton();
      }).catch(error => {
        const errorMessage = ApiHandler.getErrorMessage(error);
            console.log(errorMessage);
      });
    }

    findThatComment(chat){
       //this.setState({comments:[...this.state.comments, newChat]});
      let newComments = [...this.state.comments], i = 0;

      for(let comment of newComments){
        i++;
        if(comment.uuid === chat.uuid){
          newComments.splice(i-1,1);
          break;
        }
      }

      this.setState({comments:newComments})
    }

    onClickDeleteBtn(chat){
      this.findThatComment(chat);

      let request = ApiHandler.attachHeader({commentId: chat.uuid});
      this.props.actions.deleteComment(request, this.props.currentGroup.uuid).then(response => {
        // this.loadGroupComments();
      }).catch(error => {
        const errorMessage = ApiHandler.getErrorMessage(error);
            console.log(errorMessage);
      });
    }


  render() {
    let showMessage = <div/>,
        that = this;
      //renderin the comments of the circle if exists

      if(this.state.comments.length !== 0){
        showMessage = this.state.comments.map((chat,index,array)=>{
            return (
                <Comments
                  showSpinner={this.state.showSpinner}
                  ref={(el) => { this.commentContainer = el; }}
                  deleteComment={(chat) => this.onClickDeleteBtn(chat)}
                  key={chat.uuid}
                  chat={chat}
                  nextComment={array[index-1] ? array[index-1] : {}}
                  currentGroup={that.state.currentGroup}
                  currentUser={that.props.currentUser}
                  index={index}
                  length={this.state.comments.length}
                  media={groupUtiles.getAllChatMedia(this.state.comments)} />
            )
        });
      } else {
        showMessage = (
          <div className="text-center">
            <p className="no-comment">No past comments</p>
          </div>
        )
      }

    return (
      <div >
          <div className="circle-container">
              <TopBar
                groupId={this.state.groupId}
                currentGroup={this.state.currentGroup}
                hideInfo={() => this.props.hideInfo()}/>

              <div ref={(el) => { this.messagesContainer = el; }}
                    className="chat-container"
                     style={{height:`calc(100vh - 240px)`}}>
                    {showMessage}

                </div>
                <InputSendComponent
                  ref={(el) => {this.inputContainer = el; }}
                  groupId={this.state.groupId}
                  onClickJoinCircle={this.onClickJoinCircle}
                  appendComment={this.appendComment}
                  scrollToBottom={() => this.scrollToBottom()}
                  adjustHeightWithPhoto={(height) => this.adjustHeightWithPhoto(height)}/>
            </div>
          </div>
        );
    }
}


ChatComponent.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {
    return {
      groupId: state.group.groupId,
      currentUser: state.user.currentUser,
      comments: state.comments.comments,
      currentGroup: state.group.currentGroup,
      socketUrl: state.group.socketUrl,
      savedGroups: state.groups.savedGroups,
    }
}

function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators({...userActions,...groupActions, ...commentActions,...groupsActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(ChatComponent);
