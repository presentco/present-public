import React, { Component} from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as userActions from '../../../../redux/actions/userActions';
import * as groupActions from '../../../../redux/actions/groupActions';
import * as groupsActions from '../../../../redux/actions/groupsActions';
import * as commentActions from '../../../../redux/actions/commentActions';
import './style.css';
import _ from 'lodash';
import userUtiles from '../../../../core/userUtiles';
import ApiHandler from '../../../../core/ApiHandler';
import MapComponent from './Components/MapComponent';
import Modal from '../Modal';
import Description from './Components/Description';
import GroupCategories from './Components/Categories';
import Members from './Components/Members';
import Media from './Components/Media';
import Privacy from './Components/Privacy';
import CircleOptions from './Components/CircleOptions';
import CircleCoverPhoto from './Components/CircleCoverPhoto';
import EditCircle from '../../components/chatComponent/Components/EditCircle';
import Spinner from 'halogen/BounceLoader';


class GeneralComponent extends Component {

  constructor(props){

    super(props);
    this.state={
      savedGroups:{},
      currentGroup:props.currentGroup,
      modalMap:false,
      members:[],
      currentUser:props.currentUser
    }

    this.onClickSaveCircle = this.onClickSaveCircle.bind(this);
    this.saveEditedCircleWithPhoto = this.saveEditedCircleWithPhoto.bind(this);
  }

  componentWillReceiveProps(nextProps){
    if (this.props.currentGroup !== nextProps.currentGroup) {

        this.setState({currentGroup: nextProps.currentGroup });
        if(userUtiles.isUserCircleOwner(this.props.currentUser, nextProps.currentGroup) || userUtiles.isUserAdmin(this.props.currentUser)){
          this.getMembershipRequests(nextProps.currentGroup.uuid);
        }
    }

    if(this.props.savedGroups !== nextProps.savedGroups){
      this.setState({savedGroups: nextProps.savedGroups});
    }

    if(this.props.currentUser !== nextProps.currentUser){
      this.setState({currentUser: nextProps.currentUser});
    }
  }

  getMembershipRequests(groupId){

    this.props.actions.getMembershipRequests(groupId).then(requests => {
      this.setState({requests});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
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

    this.props.actions.updateGroupInfo(request).then(response => {

      this.setState({circleLoading:false, showEditModal:false})
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
          console.log(errorMessage);
    });
  }

  getGroupMembers(group){

    let request = ApiHandler.attachHeader({groupId: group})
    this.props.actions.getGroupMembers(request).then(response => {
      if(!response.error){
        this.setState({members: response.result.members});
      }
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  saveEditedCircleWithPhoto(updatedCircle, cover){
    let newCircle = updatedCircle;
    newCircle.center = updatedCircle.location;
    newCircle.createdFrom = updatedCircle.location;
    newCircle.radius = this.props.currentGroup.radius;

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

        this.setState({circleLoading:false, showEditModal:false})
      }).catch(error => {

        const errorMessage = ApiHandler.getErrorMessage(error);
            console.log(errorMessage);
      });
    }).catch(error => {
         const errorMessage = ApiHandler.getErrorMessage(error);
         console.log(errorMessage);
      });
    }

    getCurrentGroup(){
      this.props.getCurrentGroup()
    }

    renderEditCircleModal(){
      return(
        <Modal
          show={this.state.showEditModal}
          closeModal={() => this.setState({showEditModal:false})}
          noPadding={true}
          width={true}
          noBoxShadow={this.state.circleLoading ? true : false}
          height={'300px'}>
          {this.state.circleLoading ?
            <div className="spinner-container">
              <Spinner color="#8136ec" size="150px" margin="100px"/>
            </div> :
          <EditCircle
            yourCircle={this.props.currentGroup}
            closeEditModal={() => this.setState({showEditModal: false})}
            saveEditedCircle={this.onClickSaveCircle}
            saveEditedCircleWithPhoto={this.saveEditedCircleWithPhoto}
            type="edit-circle"/>
        }
        </Modal>
      )
    }

  render() {
    let showMap = <div/>,
        modalMap = <div/>,
        EditModal = <span/>,
        currentGroup = this.props.currentGroup;

        if(!_.isEmpty(currentGroup)){
          showMap= <div onClick={() => this.setState({modalMap:true})}><MapComponent closeMap={this.onClickCloseMap} /></div>;
          modalMap = (
            <Modal show={this.state.modalMap} closeModal={() => this.setState({modalMap:false})}>
              <div className="relative map-container">
                <button className="x-btn map-btn" onClick={() => this.setState({modalMap:false})}>X</button>
                <MapComponent isModal={true} />
              </div>
          </Modal>
          );
      };



    return (

      <div className="general-container">
        <div className="aspect-ratio">
          {currentGroup.cover ?
            <CircleCoverPhoto currentGroup={currentGroup}/>
            :
            <div>
              <img alt={currentGroup.title}
                  className="circle-photo"
                  src={require('../../../../assets/images/placeholder.png')}/>
            </div>
          }
        </div>
        {this.state.showEditModal ? this.renderEditCircleModal() : <span/>}
        {modalMap}
        <CircleOptions
          savedGroups={this.state.savedGroups}
          currentGroup={this.state.currentGroup}
          didUserJoinedCircle={userUtiles.userJoinedGroup(this.state.currentGroup.uuid, this.state.savedGroups)}
          currentUser={this.state.currentUser}
          getCurrentGroup={() => this.getCurrentGroup()}
          members={this.state.members}
          isUserAuthorized={!_.isEmpty(this.state.currentGroup) && (userUtiles.isUserCircleOwner(this.props.currentUser, this.state.currentGroup) || userUtiles.isUserAdmin(this.props.currentUser))}
          getMembershipRequests={(uuid) => this.getMembershipRequests(uuid)}
          />
        <div className="fake-distance"></div>
        <div className="get-background">
          <Members
            currentUser={this.props.currentUser}
            currentGroup={this.props.currentGroup}
            members={this.state.members}
            getMembershipRequests={(uuid) => this.getMembershipRequests(uuid)}
            getMembers={(members) => this.setState({members})}/>
        </div>

        <Description
          currentUser={this.props.currentUser}
          currentGroup={this.state.currentGroup}/>
        {this.props.media.length !== 0 ?
          <Media
            currentGroup={this.props.currentGroup}
            media={this.props.media} />
          : <span/>
        }

        <Privacy currentGroup={this.props.currentGroup}/>
        <div className="description-map-container">
          <p className="title-gen location-tit" onClick={() => this.setState({modalMap:true})}>Location</p>
          <div onClick={() => this.setState({modalMap:true})} className="cursor-poiner">
            <img alt="location" className="city-icon loc-i on-top" src={require('../../../../assets/images/page-1.svg')} />
            <p className="location-name">{this.props.currentGroup.locationName}</p>
          </div>
            {showMap}
        </div>
        {!_.isEmpty(this.props.currentGroup) && this.props.currentGroup.categories.length > 0 ?
          <GroupCategories
            currentUser={this.props.currentUser}
            currentGroup={this.props.currentGroup}/>
        : <span/>}

      </div>
      );
    }
}

function mapStateToProps(state, ownProps) {
    return {
      groupId: state.group.groupId,
      currentUser: state.user.currentUser,
      comments: state.comments.comments,
      currentGroup: state.group.currentGroup,
      memberRequests: state.group.memberRequests
    }
}

function mapDispatchToProps(dispatch) {
    return {
        actions: bindActionCreators({...userActions,...groupActions,...groupsActions,...commentActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(GeneralComponent);
