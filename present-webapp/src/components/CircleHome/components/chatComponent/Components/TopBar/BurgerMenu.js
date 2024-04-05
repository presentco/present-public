import React, { Component,PropTypes } from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as userActions from '../../../../../../redux/actions/userActions';
import * as groupActions from '../../../../../../redux/actions/groupActions';
import * as groupsActions from '../../../../../../redux/actions/groupsActions';
import * as commentActions from '../../../../../../redux/actions/commentActions';
import ApiHandler from '../../../../../../core/ApiHandler';
import groupUtiles from '../../../../../../core/groupUtiles';
import EditCircle from '../../Components/EditCircle';
import Modal from '../../../Modal';
import Spinner from 'halogen/BounceLoader';

class BurgerMenu extends Component {

  constructor(props){
    super(props);
    this.state={
      burgerVisible:false,
      reportReason:false,
      thanksReport:false,
      canEdit:false
    }

    this.onClickMute= this.onClickMute.bind(this);
    this.onClickBurger= this.onClickBurger.bind(this);
    this.onClickSaveCircle = this.onClickSaveCircle.bind(this);
    this.saveEditedCircleWithPhoto = this.saveEditedCircleWithPhoto.bind(this);
    this.setWrapperRef = this.setWrapperRef.bind(this);
    this.handleClickOutside = this.handleClickOutside.bind(this);
    this.onClickConfirmDelete= this.onClickConfirmDelete.bind(this);

  }

  componentDidMount() {
    //meanwhile get past comments to se if anything added
     document.addEventListener('mousedown', this.handleClickOutside);
 }

 componentWillUnmount() {
     document.removeEventListener('mousedown', this.handleClickOutside);
 }

 setWrapperRef(node) {
     this.wrapperRef = node;
 }

 // /**
 //  * Alert if clicked on outside of element
 //  */
 handleClickOutside(event) {

     if (this.wrapperRef && !this.wrapperRef.contains(event.target)) {
         this.setState({burgerVisible:false});
     }
 }


  loadUserSavedGroups(){
    let requestTest = ApiHandler.attachHeader({
      location:{
          accuracy:0,
          latitude:37.785834,
          longitude:-122.406417
        }
        }
      ),
        that= this;
        
    this.props.actions.getSavedGroups(requestTest).then(response => {
      this.setState({
        savedGroups: response,
        muted:groupUtiles.isGroupMuted(response.mutedGroups,that.props.currentGroup)
      });

    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
          console.log(errorMessage);
    });
  }

  saveEditedCircleWithPhoto(updatedCircle, cover){
    let newCircle = updatedCircle;
    newCircle.center = updatedCircle.location;
    newCircle.createdFrom = updatedCircle.location;
    newCircle.radius = this.props.yourCircle.radius;

    let requestContent = ApiHandler.attachHeader({
        uuid: ApiHandler.guid(),
        type: 'JPEG',
        content:cover.split(',')[1]
      });

      this.setState({circleLoading:true})

    this.props.actions.uploadNewfile(requestContent).then(response => {

      newCircle.cover = {
        uuid: response.uuid,
        type: response.contentType
      };

      let request = ApiHandler.attachHeader(newCircle);

      this.props.actions.updateGroupInfo(request).then(response => {
        this.setState({circleLoading:false, canEdit:false})
      }).catch(error => {
        alert(error.data.result.error)
        const errorMessage = ApiHandler.getErrorMessage(error);
            console.log(errorMessage);
      });
    }).catch(error => {
         const errorMessage = ApiHandler.getErrorMessage(error);
         console.log(errorMessage);
      });
    }

    onClickReport(reason){
      let request = ApiHandler.attachHeader({groupId: this.props.yourCircle.uuid, reason:reason})
      this.props.actions.flagGroup(request).then(response => {
          this.setState({thanksReport:true,reportReason:false,burgerVisible:false});
      }).catch(error => {
        const errorMessage = ApiHandler.getErrorMessage(error);
            console.log(errorMessage);
      });
    }

    onClickBurger(){
      this.loadUserSavedGroups();
      this.setState({burgerVisible: !this.state.burgerVisible});
    }

    onClickMute(){
      if(!this.state.muted){
        let request = ApiHandler.attachHeader({groupId: this.props.yourCircle.uuid})
          this.props.actions.muteGroup(request).then(response => {
            this.loadUserSavedGroups();
          }).catch(error => {
            const errorMessage = ApiHandler.getErrorMessage(error);
                console.log(errorMessage);
          });
      } else {
        let request = ApiHandler.attachHeader({groupId: this.props.yourCircle.uuid})
          this.props.actions.unMuteGroup(request).then(response => {
            this.loadUserSavedGroups();
            this.setState({burgerVisible:false});
          }).catch(error => {
            const errorMessage = ApiHandler.getErrorMessage(error);
                console.log(errorMessage);
          });
      }
    }

    onClickSaveCircle(updatedCircle){
      this.setState({circleLoading:true})
      let newCircle = updatedCircle;
      newCircle.center = updatedCircle.location;
      newCircle.createdFrom = updatedCircle.location;
      newCircle.radius = this.props.yourCircle.radius;
      if(this.props.currentGroup.cover){
        newCircle.cover = {
          uuid:this.props.yourCircle.cover.uuid,
          type:this.props.yourCircle.cover.contentType,
          content: this.props.yourCircle.cover.content}
      }



      let request = ApiHandler.attachHeader(newCircle);

      if(!updatedCircle.spaceId){
        request.header.spaceId = 'everyone';
      }

      this.props.actions.updateGroupInfo(request).then(response => {
        this.setState({circleLoading:false, canEdit:false})
      }).catch(error => {
        const errorMessage = ApiHandler.getErrorMessage(error);
        console.log(errorMessage);
        this.setState({circleLoading:false, showError: true, errorMessage});
      });
    }


  renderMenuOptions(){
    return(
      <div className="burger-menu-bt-container" ref={this.setWrapperRef}>
        { this.props.canUserEditCircle ?
          <button
            onClick={() => this.setState({canEdit:true, burgerVisible:false})}
            className="burger-menu-btn">
            Edit
          </button> :
          <div/>
        }
        <button
          onClick={this.onClickMute}
          className="burger-menu-btn">
          {this.state.muted ? "Unmute" : "Mute"}
        </button>
        {  !this.props.isUserCircleOwner ?
          <button
            onClick={() => this.setState({reportReason:true,burgerVisible:false})}
            className="burger-menu-btn">
            Report
          </button> :
          <div/>
        }
        {this.props.canUserEditCircle ?
          <button
            onClick={() => this.setState({showDeleteConfirmation:true})}
            className="burger-menu-btn">
            Delete
          </button> :
          <div/>
        }

      </div>
    )
  }

  renderReportReason(){
    return(
      <Modal show={this.state.reportReason} closeModal={() => this.setState({reportReason: false})} width={true}>
        <div className="text-center ">
          <p className="confirm-text is-success">Tell us why you&apos;re reporting this circle so we can help!</p>
          <button
            onClick={()=>this.onClickReport("SPAM")}
            className="burger-menu-btn report-btn">
            <span className="info-text">It&apos;s spam</span>
          </button>
          <button
            onClick={()=> this.onClickReport("INAPPROPRIATE")}
            className="burger-menu-btn report-btn">
            <span className="info-text">It&apos;s inapproriate</span>
          </button>
          <button
            onClick={()=> this.setState({reportReason:false, burgerVisible:false})}
            className="burger-menu-btn report-btn">
            <span className="info-text">Cancel</span>
          </button>
      </div>
    </Modal>
    )
  }

  renderThanksReport(){
    return(
      <Modal show={this.state.thanksReport} closeModal={() => this.setState({thanksReport: false, burgerVisible:false})}>
        <div className="text-center">
          <p className="confirm-text is-success">Thanks, We will look into it ASAP!</p>
          <button
            onClick={()=> this.setState({thanksReport:false, burgerVisible:false})}
            className="burger-menu-btn report-btn close-btn">
            <span className="info-text">Close</span>
          </button>
      </div>
    </Modal>
    )
  }

  onClickConfirmDelete(){
    let request = ApiHandler.attachHeader({groupId:this.props.currentGroup.uuid});
    this.props.actions.deleteGroup(request).then(response => {
      this.context.router.replace('/app');
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
          console.log(errorMessage);
    });
  }

  renderDeteleConfirmation(){
    return(
      <Modal
        show={this.state.showDeleteConfirmation}
        closeModal={() => this.setState({showDeleteConfirmation:false})}>
        <img src={require('../../../../../../assets/images/close-btn-white@2x.png')}
          alt="close" className="white-x"
          onClick={() => this.setState({showDeleteConfirmation:false})}/>
        <div className="confrim-container text-center ">
          <p className="confirm-text">HOLD UP</p>
          <p className="info-text">You&apos;re about to delete this circle.</p>
          <p className="info-text sec">Are you sure?</p>
          <button className="confirm-btn" onClick={this.onClickConfirmDelete}>Yes, Please</button>
          <button className="cancel-btn" onClick={() => this.setState({showDeleteConfirmation:false})}>No, I love it here!</button>
        </div>
      </Modal>
    )
  }

  renderEditDetailsModal(){
    return(
      <Modal show={this.state.canEdit}
             noPadding={true}
             closeModal={() => this.setState({canEdit: false})}
             width={'true'}
             widthSize={'594px'}
             type={"create-circle"}

             noBoxShadow={this.state.circleLoading ? true : false}
             clickInput={this.state.clickInput}>
             {this.state.circleLoading ?
               <div className="spinner-container">
                 <Spinner color="#8136ec" size="150px" margin="100px"/>
               </div>
              :
              <EditCircle
                yourCircle={this.props.yourCircle}
                closeEditModal={() => this.setState({canEdit: false})}
                saveEditedCircle={this.onClickSaveCircle}
                saveEditedCircleWithPhoto={this.saveEditedCircleWithPhoto}
                type="edit-circle"
                clickInput={(val) => this.setState({clickInput: val})}
                errorMessage={this.state.errorMessage}/>
            }
      </Modal>
    )
  }


  render(){
    return(
      <div >
        <button className="burger-btn-i" onClick={this.onClickBurger}>
          <img
            alt="menu"
            src={require('../../../../../../assets/images/hamburger@3x.png')} />
        </button>
        {this.state.burgerVisible ? this.renderMenuOptions() : <span/>}
        {this.state.reportReason ? this.renderReportReason() : <span/>}
        {this.state.thanksReport ? this.renderThanksReport() : <span/>}
        {this.state.canEdit ? this.renderEditDetailsModal() : <span/>}
        {this.state.showDeleteConfirmation ? this.renderDeteleConfirmation() : <span/>}
      </div>
    )
  }
}

BurgerMenu.contextTypes = {
  router: PropTypes.object
};

  function mapStateToProps(state, ownProps) {
      return {
        groupId: state.group.groupId,
        currentUser: state.user.currentUser,
        currentGroup: state.group.currentGroup,
        savedGroups: state.group.savedGroups
      }
  }

  function mapDispatchToProps(dispatch) {
      return {
        actions: bindActionCreators({...userActions,...groupActions,...groupsActions,...commentActions}, dispatch)
      }
  }

  export default connect(mapStateToProps, mapDispatchToProps)(BurgerMenu);
