import React, { Component,PropTypes } from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import ReactDOM from 'react-dom';
import * as groupActions from '../../../../../../redux/actions/groupActions';
import * as groupsActions from '../../../../../../redux/actions/groupsActions';
import * as commentActions from '../../../../../../redux/actions/commentActions';
import * as userActions from '../../../../../../redux/actions/userActions';
import ApiHandler from '../../../../../../core/ApiHandler';
import './style.css';
import _ from 'lodash';
import moment from 'moment';
import Spinner from 'halogen/BeatLoader';
import Modal from '../../../Modal';
import TextareaAutosize from 'react-autosize-textarea';
import Dropzone from 'react-dropzone';

class InputSendComponent extends Component {

  constructor(props){
    super(props);
    this.state={
      comments: [],
      currentGroup: {},
      newChat:{
        comment:'',
        content:{}
      },
      content:{},
      subContent:{},
      subComment:'',
      groupId:props.groupId,
      disabledBtn:true,
      errorMsg:false,
      rows:1,
      accepted:[],
      rejected:[]
    }

    this.onChangechatValue = this.onChangechatValue.bind(this);
    this.onClickSend = this.onClickSend.bind(this);
    this._handleKey = this._handleKey.bind(this);
    this._handleKeyPress = this._handleKeyPress.bind(this);
    this.onDrop = this.onDrop.bind(this);
  }


  //Called any time the Props have Changed in the Redux Store
  componentWillReceiveProps(nextProps) {
      //Check if the Props for group have in fact changed.
      if (this.props.currentGroup !== nextProps.currentGroup) {
          this.setState({currentGroup: nextProps.currentGroup });
      }
  }

  onChangechatValue(e){

    e.preventDefault();
    if(!navigator.onLine){
        this.setState({disabledBtn:true});
    } else {
      if(e.target.value !== "" && (/\S/.test(e.target.value))){
        this.setState({disabledBtn:false});
      } else {
          this.setState({disabledBtn:true});
      }
    }

    let newChat={},
        splitCounter = (e.target.value.match(/\n/g)||[]).length;
        newChat.comment = e.target.value;

        if(splitCounter >= 1){
          this.setState({rows: splitCounter});
        }
        this.setState({newChat, subComment:e.target.value });
  }

  loadGroupComments(){
    let request = ApiHandler.attachHeader({groupId:this.props.currentGroup.uuid});
    this.props.actions.loadRequestedGroupChats(request).then( response => {
      if(response){
        this.setState({comments: response});
      }
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  onDrop(accepted, rejected){
    if(this.props.currentGroup.preapprove !== 'ANYONE' && !this.props.currentGroup.joined){
      //check if the user is member or not
      //if not a member show you should join the circle first
      this.setState({showRequestModal: true});
    } else {
    if(rejected.length > 0){
      this.setState({wrongFileUploaded: true, showLimitationPhotoUploadError:true});
    } else if(accepted.length > 1){
      this.setState({showLimitationPhotoUploadError:true});
    } else {
      let reader = new FileReader(),
          file = accepted[0],
          self = this;
          this.setState({loadingPhoto:true});
        reader.onload = function(upload) {
          // self.setState({loadingPhoto:false})
          let request = ApiHandler.attachHeader({
                uuid: ApiHandler.guid(),
                type: 'JPEG',
                content:upload.target.result.split(',')[1]
              });

          self.setState({
            request,
            content:upload.target.result,
            hasImage:true,
            subContent:upload.target.result,
            loadingPhoto:false
          });
          self.props.adjustHeightWithPhoto('calc(100vh - 290px)');
          // self.uploadPhotoToStore(self,request,newChat);
        };

        reader.readAsDataURL(file);
        self.setState({disabledBtn:false});
    }
  }

   }

  uploadPhotoToStore(newChat){
    let node = ReactDOM.findDOMNode(this.imageContainer),
     newRows = this.inputContainer.textarea.clientHeight + ((node.naturalHeight * 21.12)/100);

    this.props.appendComment(newChat, 'true', newRows);
    this.props.scrollToBottom();
    this.setState({hasImage:false, subContent:{}});
    this.props.actions.uploadNewfile(this.state.request).then( response => {
      newChat.content = {
        uuid: response.uuid,
        type:response.contentType
      };

      let newrequest = ApiHandler.attachHeader(newChat);
      //upload the new content to comments
      this.props.actions.createNewComment(newrequest, this.props.currentGroup.uuid).then( response => {
        //error handeling
        if(response.data.error){
          this.setState({errorMsg: true});
        } else {
          this.setState({newChat});
          if(!this.props.currentGroup.joined){
            this.props.onClickJoinCircle();
          }
          newChat.comment = "";
          this.setState({newChat, errorMsg:false});
        }

      }).catch(error => {
        const errorMessage = ApiHandler.getErrorMessage(error);
        console.log(errorMessage);
      });
    }).catch(error => {

      if(error){
        this.setState({comments:newChat})
      }
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }


  onClickSend(data){
    let newRows = this.inputContainer.textarea.clientHeight;

    let newChat={},
        newCurrentUser = {
          name: `${this.props.currentUser.name.first} ${this.props.currentUser.name.last}`,
          id: this.props.currentUser.id,
          bio: this.props.currentUser.bio,
          interests: this.props.currentUser.interests,
          friends:  this.props.currentUser.friends,
          photo:  this.props.currentUser.photo
        }

        newChat.uuid = ApiHandler.guid();
        newChat.groupId = this.state.currentGroup.uuid;
        newChat.location = this.state.currentGroup.location;
        newChat.author = newCurrentUser;
        newChat.creationTime = moment();
        newChat.comment = this.state.newChat.comment;
        if(!_.isEmpty(this.state.subContent)){
          newChat.content= {
            content : this.state.content
          };
        }

    this.setState({subComment:'', subContent:{}});
    //if the circle is not public
    if(this.props.currentGroup.preapprove !== 'ANYONE' && !this.props.currentGroup.joined){
      //check if the user is member or not
      //if not a member show you should join the circle first
      this.setState({showRequestModal: true});
    } else if(this.state.subContent && !_.isEmpty(this.state.subContent)){

        this.uploadPhotoToStore(newChat);
      } else {
        this.props.appendComment(newChat,'false',newRows);
        this.props.scrollToBottom();
        if(newChat.comment !== '' && (/\S/.test(newChat.comment))){
            let request = ApiHandler.attachHeader(newChat);

            this.props.actions.createNewComment(request, this.state.currentGroup.uuid).then((response) => {
              this.setState({rows:1});

              //error handeling
              if(response.data.error){
                this.setState({errorMsg: true});
              } else {
                this.setState({newChat});
                if(!this.props.currentGroup.joined){
                  this.props.onClickJoinCircle();
                }
                newChat.comment = "";
                this.setState({newChat, errorMsg:false});
              }

            }).catch(error => {

              if(error){
                this.setState({comments:newChat})
              }
              const errorMessage = ApiHandler.getErrorMessage(error);
              console.log(errorMessage);
            });
        }
      }
  }

  _handleKey(e){
    let comment = this.state.newChat.comment,
        newChat = this.state.newChat;
    let splitCounter;

    if((e.altKey && e.key === 'Enter') || (e.shiftKey && e.key === 'Enter')){
        comment += "\n";
        splitCounter = (comment.match(/\n/g)||[]).length;
        newChat.comment = comment;
        this.setState({rows:splitCounter, newChat, subComment:comment});
    } else {
      if((this.state.newChat.comment !== '' && (/\S/.test(this.state.newChat.comment))) || this.state.hasImage){
        if(e.key === 'Enter'){
            e.preventDefault();
            this.onClickSend();
        }
      }
    }
  }

  _handleKeyPress(e){
    let comment = this.state.newChat.comment;

    if(comment === '' && this.state.hasImage){
      if(e.key === 'Backspace'){
        this.setState({hasImage:false});
        this.props.adjustHeightWithPhoto('calc(100vh - 240px)');
      }
    }
  }

  renderRequestModal(){
    return(
      <Modal show={this.state.showRequestModal}
             closeModal={() => this.setState({showRequestModal: false})}>
             <img src={require('../../../../../../assets/images/close-btn-white@2x.png')}
                  alt="close" className="white-x"
                  onClick={() => this.setState({showRequestModal:false, hasImage:false})}/>
                <div className="tonfrim-container text-center">
               <p className="confirm-text member-confirm-text">HOLD UP</p>
               <p className="info-text member-text">You should be member of this circle to start a conversation!</p>
             </div>
      </Modal>
    )
  }


  render() {

    let showLimitationPhotoUploadError=<span/>;

    if(this.state.showLimitationPhotoUploadError){
      showLimitationPhotoUploadError=(
        <Modal show={this.state.showLimitationPhotoUploadError}
               width={true}
               widthSize={true}
               widthTrue={true}
               height={true}
               closeModal={() => this.setState({showLimitationPhotoUploadError: false,wrongFileUploaded:false})}>
               <img src={require('../../../../../../assets/images/close-btn-white@2x.png')}
                    alt="close" className="white-x"
                    onClick={() => this.setState({showLimitationPhotoUploadError:false,wrongFileUploaded:false})}/>
               <div className="confrim-container text-center ">
                 <p className="confirm-text">HOLD UP</p>
                 <p className="info-text">{this.state.wrongFileUploaded ? "Sorry, only jpeg/png files are supported!" : "Please upload one photo at a time!"}</p>
               </div>
        </Modal>
      )
    }
    return (

        <div className="send-container">


        {showLimitationPhotoUploadError}
        {this.state.showRequestModal ? this.renderRequestModal(): <span/>}
          <div className="main-send-container">
             <Dropzone accept="image/jpeg, image/png" onDrop={this.onDrop} className="dropzone" >
                <label for="file-upload" className="custom-file-upload">
                    <img src={require('../../../../../../assets/images/fill-149.svg')} alt=""/>
                </label>
                </Dropzone>
                <span className="camera-div"></span>
                {this.state.hasImage ?
                <img alt="upload" className="uploaded-image" src={this.state.subContent} ref={(el) => {this.imageContainer = el;}}/> : <span/>}
                  {this.state.loadingPhoto ?
                    <div className="spinner-input-loader text-center">
                      <Spinner color="#8136ec" size="10px" />
                    </div>

                 :  <span/>}
               <Dropzone accept="image/jpeg, image/png" onDrop={this.onDrop}  className="dropzone-for-input" disableClick={true} >
                <TextareaAutosize
                  ref={(el) => { this.inputContainer = el; }}
                  autoFocus
                  name="comment"
                  onKeyDown={this._handleKeyPress}
                  value={this.state.subComment}
                  placeholder="Say Something"
                  className="input-chat"
                  onChange={this.onChangechatValue}
                  onKeyPress={this._handleKey}
                  wrap="soft"/>

              </Dropzone>
                {this.state.errorMsg ?
                  <div className="text-center">
                    <p className="not-sent-error">!</p>
                  </div>
                 :
                <span/>}
            <button className="send-btn" disabled={this.state.disabledBtn} onClick={this.onClickSend}>{this.state.errorMsg ? "Resend" : "Send"}</button>
          </div>
        </div>
      );
    }
}


InputSendComponent.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {
    return {
      currentGroup: state.group.currentGroup,
      currentUser: state.user.currentUser,
      savedGroups: state.groups.savedGroups
    }
}

function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators({userActions,...groupActions, ...commentActions,...groupsActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(InputSendComponent);
